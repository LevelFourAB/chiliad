package se.l4.chiliad.engine;

import reactor.core.publisher.Mono;

/**
 * Builder used when setting up access to remote services from an instance of
 * {@link Chiliad}.
 *
 * @param <T>
 */
public interface ServiceBuilder<T>
{
	/**
	 * Get a mono that will return this instance, but only once it is available
	 * from at least one peer.
	 *
	 * @return
	 */
	Mono<T> whenAvailable();

	/**
	 * Build the instance.
	 *
	 * @return
	 */
	Mono<T> build();
}
