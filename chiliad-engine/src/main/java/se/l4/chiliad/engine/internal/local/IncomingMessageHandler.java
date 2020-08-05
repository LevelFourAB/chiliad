package se.l4.chiliad.engine.internal.local;

import io.netty.buffer.ByteBuf;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.ByteBufPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.internal.protocol.InvocationPayloadDecoding;
import se.l4.chiliad.engine.internal.protocol.InvocationPayloadEncoding;
import se.l4.chiliad.engine.spi.InvokableRequestResponseMethod;
import se.l4.chiliad.engine.spi.InvokableRequestStreamMethod;
import se.l4.chiliad.engine.spi.InvokableService;
import se.l4.chiliad.engine.spi.InvokableServiceMethod;
import se.l4.chiliad.engine.transport.TransportException;

/**
 * Class responsible for taking incoming payloads, decoding and routing them
 * to the correct {@link InvokableService}.
 */
public class IncomingMessageHandler
	implements RSocket
{
	public final ServiceRegistry registry;

	public IncomingMessageHandler(
		ServiceRegistry registry
	)
	{
		this.registry = registry;
	}

	private <T extends InvokableServiceMethod> Mono<T> resolveServiceAndMethod(
		Payload payload,
		Class<T> type
	)
	{
		ByteBuf metadata = payload.metadata();

		String service = InvocationPayloadDecoding.getService(metadata);
		InvokableService invokable = registry.get(service);
		if(invokable == null)
		{
			return Mono.error(new TransportException("Service with id " + service + " is not available"));
		}

		String method = InvocationPayloadDecoding.getMethod(metadata);
		return invokable.getMethod(method)
			.filter(m -> type.isAssignableFrom(m.getClass()))
			.cast(type);
	}

	@Override
	public Mono<Payload> requestResponse(Payload payload)
	{
		return resolveServiceAndMethod(payload, InvokableRequestResponseMethod.class)
			.flatMap(method -> {
				try
				{
					Object[] args = InvocationPayloadDecoding.getArguments(
						method.getContract().getArgumentCodecs(),
						payload.data()
					);

					return method.invoke(args)
						.map(data -> InvocationPayloadEncoding.encodeObject(method.getContract().getResponseCodec(), data));
				}
				finally
				{
					payload.release();
				}
			})
			.map(data -> ByteBufPayload.create(data));
	}

	@Override
	public Flux<Payload> requestStream(Payload payload)
	{
		return resolveServiceAndMethod(payload, InvokableRequestStreamMethod.class)
			.flatMapMany(method -> {
				try
				{
					Object[] args = InvocationPayloadDecoding.getArguments(
						method.getContract().getArgumentCodecs(),
						payload.data()
					);

					return method.invoke(args)
						.map(data -> InvocationPayloadEncoding.encodeObject(method.getContract().getResponseCodec(), data));
				}
				finally
				{
					payload.release();
				}
			})
			.map(data -> ByteBufPayload.create(data));
	}
}
