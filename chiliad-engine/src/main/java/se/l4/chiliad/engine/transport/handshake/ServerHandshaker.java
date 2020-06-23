package se.l4.chiliad.engine.transport.handshake;

import java.util.Optional;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.auth.AuthMethod;
import se.l4.chiliad.engine.auth.AuthOk;
import se.l4.chiliad.engine.auth.AuthReject;
import se.l4.chiliad.engine.auth.AuthServerFlow;
import se.l4.chiliad.engine.transport.TransportContext;
import se.l4.chiliad.engine.transport.TransportException;

/**
 * Handshake helper for the server-side handshake. This will let a server
 * register messages from a client and receive information about what to send
 * back to a client.
 */
public class ServerHandshaker
{
	private enum State
	{
		WAITING_FOR_SELECT,
		WAITING_FOR_AUTH,
		WAITING_FOR_AUTHDATA,
		WAITING_FOR_BEGIN
	}

	private final TransportContext context;

	private State state;
	private AuthServerFlow authFlow;

	public ServerHandshaker(
		TransportContext context
	)
	{
		this.context = context;
		this.state = State.WAITING_FOR_SELECT;
	}

	/**
	 * Get the initial message to send.
	 *
	 * @return
	 */
	public Mono<HandshakeMessage> getInitial()
	{
		return Mono.just(new Hello(new String[0]));
	}

	/**
	 * Receive the given message, resolving what to send back to the client.
	 *
	 * @param message
	 * @return
	 */
	public Mono<HandshakeMessage> receive(HandshakeMessage message)
	{
		switch(state)
		{
			case WAITING_FOR_SELECT:
				if(message instanceof Select)
				{
					Select select = (Select) message;
					if(select.getCapabilities().length != 0)
					{
						// There are no capabilities supported
						return Mono.just(Reject.INSTANCE);
					}

					state = State.WAITING_FOR_AUTH;
					return Mono.just(Ok.INSTANCE);
				}
				else
				{
					return Mono.error(new TransportException("Expected a Select, but received " + message));
				}
			case WAITING_FOR_AUTH:
				if(message instanceof Auth)
				{
					return handleAuth((Auth) message);
				}
				else
				{
					return Mono.error(new TransportException("Expected a Auth, but received " + message));
				}
			case WAITING_FOR_AUTHDATA:
				if(message instanceof AuthData)
				{
					return handleAuthData(((AuthData) message).getData());
				}
				else if(message instanceof Auth)
				{
					return handleAuth((Auth) message);
				}
				else
				{
					return Mono.error(new TransportException("Expected a AuthData, but received " + message));
				}
			case WAITING_FOR_BEGIN:
				if(message instanceof Begin)
				{
					return Mono.just(Begin.INSTANCE);
				}
				else
				{
					return Mono.error(new TransportException("Expected a Being, but received " + message));
				}
		}

		return Mono.error(new TransportException("Unknown type of message " + message));
	}

	private Mono<HandshakeMessage> handleAuth(Auth auth)
	{
		Optional<AuthMethod> method = context.findMethod(auth.getMethod());
		if(! method.isPresent())
		{
			return Mono.just(Reject.INSTANCE);
		}

		return method.get().createServer()
			.doOnNext(flow -> authFlow = flow)
			.flatMap(flow -> handleAuthData(auth.getData()));
	}

	private Mono<HandshakeMessage> handleAuthData(ByteBuf authData)
	{
		return authFlow.receiveData(authData)
			.flatMap(data -> {
				if(data instanceof AuthOk)
				{
					/*
						* Authentication is ok, next step is to wait
						* for a BEGIN.
						*/
					state = State.WAITING_FOR_BEGIN;

					return authFlow.dispose()
						.thenReturn(Ok.INSTANCE);
				}
				else if(data instanceof AuthReject)
				{
					/*
						* Authentication was rejected. Reset and wait
						* for the next auth.
						*/
					state = State.WAITING_FOR_AUTH;

					return authFlow.dispose()
						.thenReturn(Reject.INSTANCE);
				}
				else if(data instanceof AuthData)
				{
					/*
						* Need more authentication data.
						*/
					state = State.WAITING_FOR_AUTHDATA;
					return Mono.just(new se.l4.chiliad.engine.transport.handshake.AuthData(
						((AuthData) data).getData()
					));
				}
				else
				{
					return Mono.just(Reject.INSTANCE);
				}
			});
	}
}
