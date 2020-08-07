package se.l4.chiliad.engine.internal.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.chiliad.ServiceException;
import se.l4.chiliad.engine.ServiceDefinitionException;
import se.l4.chiliad.engine.spi.InvokableService;
import se.l4.chiliad.engine.spi.InvokableServiceMethod;
import se.l4.chiliad.engine.spi.MethodInvoker;
import se.l4.chiliad.engine.spi.RequestResponseMethod;
import se.l4.chiliad.engine.spi.RequestStreamMethod;
import se.l4.chiliad.engine.spi.ServiceContract;
import se.l4.chiliad.engine.spi.ServiceMethod;
import se.l4.ylem.types.reflect.MethodRef;
import se.l4.ylem.types.reflect.TypeRef;
import se.l4.ylem.types.reflect.Types;

/**
 * Generator of {@link InvokableService} from an object instance, will use
 * reflection to invoke the methods.
 */
public class LocalServiceGenerator
{
	public final ServiceContractGenerator contracts;

	public LocalServiceGenerator(
		ServiceContractGenerator contracts
	)
	{
		this.contracts = contracts;
	}

	/**
	 * Take an object instance and transform it into a {@link InvokableService}.
	 * Uses {@link ServiceContractGenerator} to generate a contract and then
	 * goes through and adds a reflection-based implementation for each method.
	 *
	 * @param instance
	 * @return
	 */
	public InvokableService generate(Object instance)
	{
		TypeRef type = Types.reference(instance.getClass());

		ServiceContract contract = contracts.generate(type);
		ServiceContract.ImplementationBuilder builder = contract.implement();

		for(MethodRef method : type.getMethods())
		{
			ServiceMethod serviceMethod = contract.getMethod(method.getName())
				.block();

			if(serviceMethod == null) continue;

			InvokableServiceMethod invokable;
			if(serviceMethod instanceof RequestResponseMethod)
			{
				if(method.getReturnType().isErasedType(Mono.class))
				{
					invokable = ((RequestResponseMethod) serviceMethod)
						.toInvokable(requestResponse(instance, method.getMethod()));
				}
				else
				{
					invokable = ((RequestResponseMethod) serviceMethod)
						.toInvokable(requestResponseImperative(instance, method.getMethod()));
				}
			}
			else if(serviceMethod instanceof RequestStreamMethod)
			{
				invokable = ((RequestStreamMethod) serviceMethod)
					.toInvokable(requestStream(instance, method.getMethod()));
			}
			else
			{
				throw new ServiceDefinitionException("Unsupported type of method: " + serviceMethod);
			}

			builder = builder.addMethod(invokable);
		}

		return builder.build();
	}

	@SuppressWarnings("unchecked")
	private static MethodInvoker<Mono<? extends Object>> requestResponse(
		Object instance,
		Method method
	)
	{
		return args -> {
			try
			{
				return (Mono<Object>) method.invoke(instance, args);
			}
			catch(IllegalAccessException e)
			{
				throw new ServiceDefinitionException("Method was not accessible; " + e.getMessage(), e);
			}
			catch(IllegalArgumentException e)
			{
				throw new ServiceException("Method received invalid arguments; " + e.getMessage(), e);
			}
			catch(InvocationTargetException e)
			{
				throw new ServiceException("Unable to call method; " + e.getCause().getMessage(), e.getCause());
			}
		};
	}

	private static MethodInvoker<Mono<? extends Object>> requestResponseImperative(
		Object instance,
		Method method
	)
	{
		return args -> Mono.fromSupplier(() -> {
			try
			{
				return method.invoke(instance, args);
			}
			catch(IllegalAccessException e)
			{
				throw new ServiceDefinitionException("Method was not accessible; " + e.getMessage(), e);
			}
			catch(IllegalArgumentException e)
			{
				throw new ServiceException("Method received invalid arguments; " + e.getMessage(), e);
			}
			catch(InvocationTargetException e)
			{
				throw new ServiceException("Unable to call method; " + e.getCause().getMessage(), e.getCause());
			}
		});
	}

	@SuppressWarnings("unchecked")
	private static MethodInvoker<Flux<? extends Object>> requestStream(
		Object instance,
		Method method
	)
	{
		return args -> {
			try
			{
				return (Flux<Object>) method.invoke(instance, args);
			}
			catch(IllegalAccessException e)
			{
				throw new ServiceDefinitionException("Method was not accessible; " + e.getMessage(), e);
			}
			catch(IllegalArgumentException e)
			{
				throw new ServiceException("Method received invalid arguments; " + e.getMessage(), e);
			}
			catch(InvocationTargetException e)
			{
				throw new ServiceException("Unable to call method; " + e.getCause().getMessage(), e.getCause());
			}
		};
	}
}
