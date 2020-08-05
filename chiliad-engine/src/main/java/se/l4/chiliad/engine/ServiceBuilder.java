package se.l4.chiliad.engine;

import java.util.function.Function;

import se.l4.chiliad.engine.spi.InvokableService;

public interface ServiceBuilder<T>
{
	/**
	 * Set the factory that will be used to create the service.
	 *
	 * @param factory
	 * @return
	 */
	ServiceBuilder<T> withFactory(Function<InvokableService, T> factory);

	/**
	 * Build the instance.
	 *
	 * @return
	 */
	T build();
}
