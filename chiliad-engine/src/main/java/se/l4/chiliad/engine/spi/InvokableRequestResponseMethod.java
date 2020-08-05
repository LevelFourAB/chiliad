package se.l4.chiliad.engine.spi;

import reactor.core.publisher.Mono;

public interface InvokableRequestResponseMethod
	extends InvokableServiceMethod
{
	@Override
	RequestResponseMethod getContract();

	/**
	 * Invoke this method.
	 *
	 * @param args
	 *   arguments of the method
	 * @return
	 *   {@link Mono} that will complete when the response is available
	 */
	Mono<? extends Object> invoke(Object... args);
}
