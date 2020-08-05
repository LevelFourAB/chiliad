package se.l4.chiliad.engine.spi;

import java.util.function.Function;

import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.internal.spi.InvokableRequestResponseMethodImpl;
import se.l4.chiliad.engine.internal.spi.RequestResponseMethodImpl;
import se.l4.exobytes.Serializer;

/**
 * Method that follows a request and response pattern where a request generates
 * a single item as its response.
 *
 * @param <T>
 */
public interface RequestResponseMethod
	extends ServiceMethod
{
	/**
	 * Get the serializer used to convert responses to and from objects.
	 *
	 * @return
	 */
	Serializer<?> getResponseCodec();

	/**
	 * Get the serializer used for arguments of this method.
	 *
	 * @return
	 */
	Serializer<?>[] getArgumentCodecs();

	/**
	 * Turn this method into an invokable instance.
	 *
	 * @param invoker
	 * @return
	 */
	default InvokableRequestResponseMethod toInvokable(Function<Object[], Mono<? extends Object>> invoker)
	{
		return new InvokableRequestResponseMethodImpl(this, invoker);
	}

	/**
	 * Start building a new method with the given name.
	 *
	 * @param name
	 * @return
	 */
	static Builder create(String name)
	{
		return RequestResponseMethodImpl.create(name);
	}

	interface Builder
	{
		/**
		 * Set the codec used for the result of the invocation.
		 *
		 * @param codec
		 * @return
		 */
		Builder withResponseSerializer(Serializer<?> codec);

		/**
		 * Set the codecs used for all of the arguments.
		 *
		 * @param codecs
		 * @return
		 */
		Builder withArgumentSerializers(Serializer<?>... codecs);

		/**
		 * Build this instance.
		 *
		 * @return
		 */
		RequestResponseMethod build();
	}
}
