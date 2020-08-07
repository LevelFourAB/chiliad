package se.l4.chiliad.engine.internal.reflection;

import java.lang.reflect.Method;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import se.l4.chiliad.engine.ServiceDefinitionException;
import se.l4.chiliad.engine.internal.remote.RSocketSupplier;
import se.l4.chiliad.engine.internal.remote.RemoteInvokers;
import se.l4.chiliad.engine.spi.MethodInvoker;
import se.l4.chiliad.engine.spi.RequestResponseMethod;
import se.l4.chiliad.engine.spi.RequestStreamMethod;
import se.l4.chiliad.engine.spi.ServiceContract;
import se.l4.chiliad.engine.spi.ServiceMethod;
import se.l4.ylem.types.reflect.MethodRef;
import se.l4.ylem.types.reflect.TypeRef;

/**
 * Class that can generate implementations of a {@link se.l4.chiliad.Service}
 * interface that will call a remote peer.
 */
public class RemoteServiceGenerator
{
	private final ServiceContractGenerator contracts;

	public RemoteServiceGenerator(
		ServiceContractGenerator contracts
	)
	{
		this.contracts = contracts;
	}

	/**
	 * Generate an implementation of a remote service.
	 *
	 * @param <T>
	 * @param type
	 * @param service
	 * @param supplier
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> T toService(
		TypeRef type,
		String service,
		RSocketSupplier supplier
	)
	{
		// Generate the service contract to figure out what methods we should implement
		ServiceContract contract = contracts.generate(type);

		try
		{
			DynamicType.Builder builder = new ByteBuddy()
				.subclass(type.getErasedType());

			Method applyMethod = MethodInvoker.class.getMethod("invoke", Object[].class);

			for(MethodRef method : type.getMethods())
			{
				ServiceMethod serviceMethod = contract.getMethod(method.getName())
					.block();

				if(serviceMethod == null)
				{
					if(method.isAbstract())
					{
						// Abstract methods are not supported so error
						throw new ServiceDefinitionException("The method " + method.toDescription() + " is abstract, but is not a remote method");
					}

					continue;
				}

				// Resolve the invoker to use for this method
				MethodInvoker invoker;
				if(serviceMethod instanceof RequestResponseMethod)
				{
					invoker = RemoteInvokers.requestResponse(service, (RequestResponseMethod) serviceMethod, supplier);
				}
				else if(serviceMethod instanceof RequestStreamMethod)
				{
					invoker = RemoteInvokers.requestStream(service, (RequestStreamMethod) serviceMethod, supplier);
				}
				else
				{
					throw new ServiceDefinitionException("Unsupported method: " + method);
				}

				// Implement the method by delegating to the MethodInvoker
				builder = builder.method(ElementMatchers.is(method.getMethod()))
					.intercept(
						MethodCall.invoke(applyMethod)
							.on(invoker, MethodInvoker.class)
							.withArgumentArray()
							.withAssigner(Assigner.GENERICS_AWARE, Assigner.Typing.DYNAMIC)
					);
			}

			// Add a toString method
			builder = builder.method(ElementMatchers.isToString())
				.intercept(FixedValue.value(type.getErasedType().getSimpleName() + "{serviceId=" + service + "}"));

			// Define the class and create an instance
			Class createdClass = builder.make()
				.load(type.getErasedType().getClassLoader())
				.getLoaded();

			return (T) createdClass.getConstructor().newInstance();
		}
		catch(Throwable e)
		{
			throw new ServiceDefinitionException("Could not create remote service; " + e.getMessage(), e);
		}
	}
}
