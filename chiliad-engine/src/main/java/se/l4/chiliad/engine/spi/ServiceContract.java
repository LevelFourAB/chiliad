package se.l4.chiliad.engine.spi;

import org.eclipse.collections.api.list.ListIterable;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.internal.spi.ServiceContractImpl;

/**
 * Contract for the methods a service is expected to have.
 */
public interface ServiceContract
{
	/**
	 * Get the name of this service.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Get all of the methods that this contract requires.
	 *
	 * @return
	 */
	ListIterable<ServiceMethod> getMethods();

	/**
	 * Get a method using the name of it.
	 *
	 * @param name
	 * @return
	 */
	Mono<ServiceMethod> getMethod(String name);

	/**
	 * Start building an {@link InvokableService} for this contract.
	 *
	 * @return
	 *   builder
	 */
	ImplementationBuilder implement();

	/**
	 * Start creating the contract of a service with the given name.
	 *
	 * @param name
	 *   the name of the service
	 * @return
	 *   builder for creating the service
	 */
	static Builder create(String name)
	{
		return ServiceContractImpl.create(name);
	}

	interface Builder
	{
		/**
		 * Add a method to this contract.
		 *
		 * @param method
		 * @return
		 */
		Builder addMethod(ServiceMethod method);

		/**
		 * Build the contract.
		 *
		 * @return
		 */
		ServiceContract build();
	}

	interface ImplementationBuilder
	{
		/**
		 * Add a fully built invokable method.
		 *
		 * @param method
		 * @return
		 */
		ImplementationBuilder addMethod(InvokableServiceMethod method);

		/**
		 * Add an implementation for a {@link RequestResponseMethod} of the
		 * given name.
		 *
		 * @param name
		 *   the name of the method
		 * @param invoker
		 *   the function to call when invoking
		 */
		ImplementationBuilder requestResponse(String name, MethodInvoker<Mono<? extends Object>> invoker);

		/**
		 * Add an implementation for a {@link RequestStreamMethod} of the given
		 * name.
		 *
		 * @param name
		 *   the name of the method
		 * @param invoker
		 *   the function to call when invoking
		 * @return
		 */
		ImplementationBuilder requestStream(String name, MethodInvoker<Flux<? extends Object>> invoker);

		/**
		 * Build the invokable service.
		 *
		 * @return
		 */
		InvokableService build();
	}
}
