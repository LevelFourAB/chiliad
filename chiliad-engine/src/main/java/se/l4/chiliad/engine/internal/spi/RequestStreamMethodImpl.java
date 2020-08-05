package se.l4.chiliad.engine.internal.spi;

import java.util.Objects;

import se.l4.chiliad.engine.spi.RequestStreamMethod;
import se.l4.exobytes.Serializer;

/**
 * Implementation of {@link RequestStreamMethod}.
 */
public class RequestStreamMethodImpl
	implements RequestStreamMethod
{
	protected final String name;

	protected final Serializer<?> responseSerializer;
	protected final Serializer<?>[] argumentSerializers;

	private RequestStreamMethodImpl(
		String name,
		Serializer<?> responseCodec,
		Serializer<?>[] argumentCodecs
	)
	{
		this.name = name;

		this.responseSerializer = responseCodec;
		this.argumentSerializers = argumentCodecs;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public Serializer<?> getResponseCodec()
	{
		return responseSerializer;
	}

	@Override
	public Serializer<?>[] getArgumentCodecs()
	{
		return argumentSerializers;
	}

	public static Builder create(String name)
	{
		Objects.requireNonNull(name, "name must be set");

		return new Builder()
		{
			private Serializer<?> responseSerializer;
			private Serializer<?>[] argumentSerializers;

			@Override
			public Builder withResponseSerializer(Serializer<?> codec)
			{
				this.responseSerializer = Objects.requireNonNull(codec);
				return this;
			}

			@Override
			public Builder withArgumentSerializers(Serializer<?>... codecs)
			{
				this.argumentSerializers = Objects.requireNonNull(codecs);
				return this;
			}

			@Override
			public RequestStreamMethod build()
			{
				Objects.requireNonNull(responseSerializer, "responseCodec must be set");
				Objects.requireNonNull(argumentSerializers, "argumentCodecs must be set");

				return new RequestStreamMethodImpl(name, responseSerializer, argumentSerializers);
			}
		};
	}
}
