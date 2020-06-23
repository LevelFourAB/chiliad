package se.l4.chiliad.engine.transport.handshake;

import java.util.Iterator;

import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.auth.AuthClientFlow;
import se.l4.chiliad.engine.auth.AuthData;
import se.l4.chiliad.engine.auth.AuthMethod;
import se.l4.chiliad.engine.auth.AuthOk;
import se.l4.chiliad.engine.auth.AuthReject;
import se.l4.chiliad.engine.transport.TransportContext;
import se.l4.chiliad.engine.transport.TransportException;

public class ClientHandshaker
{
	private enum State
	{
		WAITING_FOR_HELLO,
		WAITING_FOR_SELECT_OK,
		WAITING_FOR_AUTH,
	}

	private final TransportContext context;
	private final ByteBufAllocator byteBufAllocator;

	private State state;
	private Iterator<AuthMethod> authMethods;
	private AuthClientFlow authFlow;

	public ClientHandshaker(
		TransportContext context,
		ByteBufAllocator byteBufAllocator
	)
	{
		this.context = context;
		this.byteBufAllocator = byteBufAllocator;

		state = State.WAITING_FOR_HELLO;
	}

	public Mono<HandshakeMessage> receive(HandshakeMessage message)
	{
		switch(state)
		{
			case WAITING_FOR_HELLO:
				if(message instanceof Hello)
				{
					// TODO: Negotiate some capabilities
					state = State.WAITING_FOR_SELECT_OK;
					return Mono.just(new Select(new String[0]));
				}
				else
				{
					return Mono.error(new TransportException("Expected a Hello, but received " + message));
				}
			case WAITING_FOR_SELECT_OK:
				if(message instanceof Ok)
				{
					// Server accepted our capabilities
					state = State.WAITING_FOR_AUTH;
					authMethods = context.getAuthMethods().iterator();
					AuthMethod method = authMethods.next();
					return method.createClient()
						.doOnNext(flow -> authFlow = flow)
						.flatMap(flow -> flow.initialMessage(byteBufAllocator))
						.map(data -> new Auth(method.getId(), data));
				}
				else if(message instanceof Reject)
				{
					// Server rejected our capabilities
					return Mono.error(new TransportException("Server rejected our capabilities"));
				}
				else
				{
					return Mono.error(new TransportException("Expected an Ok or Reject, but received " + message));
				}
			case WAITING_FOR_AUTH:
				if(message instanceof Ok)
				{
					return authFlow.dispose()
						.thenReturn(Begin.INSTANCE);
				}
				else if(message instanceof Reject)
				{
					return tryNextAuthMethod();
				}
				else if(message instanceof AuthData)
				{
					AuthData data = (AuthData) message;
					return authFlow.receiveData(data.getData())
						.flatMap(reply -> {
							if(reply instanceof AuthReject)
							{
								return tryNextAuthMethod();
							}
							else if(reply instanceof AuthData)
							{
								return Mono.just(new se.l4.chiliad.engine.transport.handshake.AuthData(
									((AuthData) reply).getData()
								));
							}
							else if(reply instanceof AuthOk)
							{
								return Mono.just(new se.l4.chiliad.engine.transport.handshake.AuthData(byteBufAllocator.buffer(0)));
							}
							else
							{
								return Mono.error(new TransportException("AuthClientFlow returned unknown result: " + data));
							}
						});
				}
				else
				{
					return Mono.error(new TransportException("Expected an Ok, Reject or AuthData, but received " + message));
				}
		}

		return Mono.error(new TransportException("Unknown state"));
	}

	private Mono<HandshakeMessage> tryNextAuthMethod()
	{
		if(! authMethods.hasNext())
		{
			return Mono.error(new TransportException("No more authentication methods to try"));
		}

		AuthMethod method = authMethods.next();
		return authFlow.dispose()
			.then(method.createClient())
			.doOnNext(flow -> authFlow = flow)
			.flatMap(flow -> flow.initialMessage(byteBufAllocator))
			.map(data -> new Auth(method.getId(), data));
	}
}
