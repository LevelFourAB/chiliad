package se.l4.chiliad.engine.internal.reflection;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.chiliad.RemoteMethod;
import se.l4.chiliad.RemoteName;
import se.l4.chiliad.ServiceException;
import se.l4.chiliad.engine.ServiceDefinitionException;
import se.l4.chiliad.engine.internal.protocol.Names;
import se.l4.chiliad.engine.spi.RequestResponseMethod;
import se.l4.chiliad.engine.spi.RequestStreamMethod;
import se.l4.chiliad.engine.spi.ServiceContract;
import se.l4.chiliad.engine.spi.ServiceMethod;
import se.l4.exobytes.Serializer;
import se.l4.exobytes.Serializers;
import se.l4.ylem.types.reflect.AnnotationLocator;
import se.l4.ylem.types.reflect.MethodRef;
import se.l4.ylem.types.reflect.TypeRef;

/**
 * Class that can inspect a type and generate a {@link ServiceContract} based
 * on the annotations.
 */
public class ServiceContractGenerator
{
	private static final Serializer<?>[] EMPTY = new Serializer<?>[0];

	private final Serializers serializers;

	public ServiceContractGenerator(
		Serializers serializers
	)
	{
		this.serializers = serializers;
	}

	/**
	 * Generate a contract by inspecting the given type and its methods for
	 * annotations.
	 *
	 * @param type
	 * @return
	 */
	public ServiceContract generate(TypeRef type)
	{
		String name = type.findAnnotation(RemoteName.class)
			.orElseThrow(() -> new ServiceException("No name defined for " + type.toTypeDescription()))
			.value();

		if(! Names.isValidServiceName(name))
		{
			throw new ServiceException("Service name of " + type.toTypeDescription() + " is not valid, was: " + name);
		}

		ServiceContract.Builder builder = ServiceContract.create(name);

		for(MethodRef ref : type.getMethods())
		{
			if(ref.findAnnotation(RemoteMethod.class).isPresent())
			{
				builder = builder.addMethod(resolve(ref));
			}
		}

		return builder.build();
	}

	public ServiceMethod resolve(MethodRef ref)
	{
		String name = ref.getAnnotation(AnnotationLocator.meta(RemoteName.class))
			.map(RemoteName::value)
			.orElse(ref.getName());

		if(! Names.isValidMethodName(name))
		{
			throw new ServiceException("Name of " + ref.toDescription() + " is not valid, was: " + name);
		}

		TypeRef returnType = ref.getReturnType();
		if(returnType.isErasedType(Mono.class))
		{
			// This might be a simple request / response method
			return resolveRequestResponse(ref, name);
		}
		else if(returnType.isErasedType(Flux.class))
		{
			return resolveRequestStream(ref, name);
		}

		throw new ServiceDefinitionException("Could not resolve contract for " + ref.toDescription());
	}

	private ServiceMethod resolveRequestResponse(MethodRef ref, String name)
	{
		TypeRef responseType = ref.getReturnType()
			.getTypeParameter(0)
			.get();

		Serializer<?> returnSerializer = serializers.get(responseType);

		Serializer<?>[] argumentSerializers = ref.getParameters()
			.collect(p -> serializers.get(p.getType()))
			.toArray(EMPTY);

		return RequestResponseMethod.create(name)
			.withResponseSerializer(returnSerializer)
			.withArgumentSerializers(argumentSerializers)
			.build();
	}

	private ServiceMethod resolveRequestStream(MethodRef ref, String name)
	{
		TypeRef responseType = ref.getReturnType()
			.getTypeParameter(0)
			.get();

		Serializer<?> returnSerializer = serializers.get(responseType);

		Serializer<?>[] argumentSerializers = ref.getParameters()
			.collect(p -> serializers.get(p.getType()))
			.toArray(EMPTY);

		return RequestStreamMethod.create(name)
			.withResponseSerializer(returnSerializer)
			.withArgumentSerializers(argumentSerializers)
			.build();
	}
}
