package se.l4.chiliad.engine.spi;

import reactor.core.publisher.Flux;

public interface InvokableRequestStreamMethod
	extends InvokableServiceMethod
{
	@Override
	RequestStreamMethod getContract();

	/**
	 * Invoke the method.
	 *
	 * @param args
	 *   arguments to pass to the method
	 * @return
	 *   {@link Flux} that will stream the result
	 */
	Flux<? extends Object> invoke(Object[] args);
}
