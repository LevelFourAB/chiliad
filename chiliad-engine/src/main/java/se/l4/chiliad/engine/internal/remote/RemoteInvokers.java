package se.l4.chiliad.engine.internal.remote;

import java.util.function.Function;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import io.rsocket.RSocket;
import io.rsocket.util.ByteBufPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.internal.protocol.InvocationPayloadDecoding;
import se.l4.chiliad.engine.internal.protocol.InvocationPayloadEncoding;
import se.l4.chiliad.engine.spi.RequestResponseMethod;
import se.l4.chiliad.engine.spi.RequestStreamMethod;
import se.l4.exobytes.Serializer;

/**
 * Invokers for remote methods, can create functions that encode and send
 * data over a {@link RSocket} and decode the received result.
 */
public class RemoteInvokers
{
	private RemoteInvokers()
	{
	}

	public static Function<Object[], Mono<? extends Object>> requestResponse(
		String service,
		RequestResponseMethod method,
		Supplier<RSocket> socket
	)
	{
		String name = method.getName();
		Serializer<?>[] argumentSerializers = method.getArgumentCodecs();
		Serializer<?> responseSerializer = method.getResponseCodec();

		return args -> Mono.defer(() -> {
			if(args.length != argumentSerializers.length)
			{
				return Mono.error(new IllegalArgumentException("Argument size mismatch, service takes " + argumentSerializers.length + " arguments"));
			}

			ByteBuf metadata = InvocationPayloadEncoding.createMetadata(service, name);
			ByteBuf data = InvocationPayloadEncoding.encodeArguments(argumentSerializers, args);

			return socket.get().requestResponse(ByteBufPayload.create(data, metadata));
		}).map(payload -> {
			try
			{
				return InvocationPayloadDecoding.readObject(responseSerializer, payload.data());
			}
			finally
			{
				payload.release();
			}
		});
	}

	public static Function<Object[], Flux<? extends Object>> requestStream(
		String service,
		RequestStreamMethod method,
		Supplier<RSocket> socket
	)
	{
		String name = method.getName();
		Serializer<?>[] argumentSerializers = method.getArgumentCodecs();
		Serializer<?> responseSerializer = method.getResponseCodec();

		return args -> Flux.defer(() -> {
			if(args.length != argumentSerializers.length)
			{
				return Mono.error(new IllegalArgumentException("Argument size mismatch, service takes " + argumentSerializers.length + " arguments"));
			}

			ByteBuf metadata = InvocationPayloadEncoding.createMetadata(service, name);
			ByteBuf data = InvocationPayloadEncoding.encodeArguments(argumentSerializers, args);

			return socket.get().requestStream(ByteBufPayload.create(data, metadata));
		}).map(payload -> {
			try
			{
				return InvocationPayloadDecoding.readObject(responseSerializer, payload.data());
			}
			finally
			{
				payload.release();
			}
		});
	}
}
