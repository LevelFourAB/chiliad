package se.l4.chiliad.engine.spi;

import reactor.core.publisher.Mono;

/**
 * Abstraction of a service that helps with invoking methods no matter if they
 * are local or remote.
 */
public interface InvokableService
{
	/**
	 * Get the name of this service.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Get a method using the name of it.
	 *
	 * @param name
	 * @return
	 */
	Mono<InvokableServiceMethod> getMethod(String name);
}
