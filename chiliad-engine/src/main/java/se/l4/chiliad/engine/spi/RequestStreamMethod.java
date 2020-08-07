package se.l4.chiliad.engine.spi;

import reactor.core.publisher.Flux;
import se.l4.chiliad.engine.internal.spi.InvokableRequestStreamMethodImpl;
import se.l4.chiliad.engine.internal.spi.RequestStreamMethodImpl;
import se.l4.exobytes.Serializer;

/**
 * Method that follows a request and stream response, allowing a method to
 * return a continuing stream of data.
 *
 * @param <T>
 */
public interface RequestStreamMethod
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
	default InvokableRequestStreamMethod toInvokable(MethodInvoker<Flux<? extends Object>> invoker)
	{
		return new InvokableRequestStreamMethodImpl(this, invoker);
	}

	/**
	 * Start building a new method with the given name.
	 *
	 * @param name
	 * @return
	 */
	static Builder create(String name)
	{
		return RequestStreamMethodImpl.create(name);
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
		RequestStreamMethod build();
	}
}
