package se.l4.chiliad.engine.auth;

import reactor.core.publisher.Mono;

public interface AuthMethod
{
	/**
	 * Get the identifier of this method.
	 *
	 * @return
	 */
	String getId();

	/**
	 * Create the flow used on the client-side for this authentication method.
	 *
	 * @return
	 */
	Mono<AuthClientFlow> createClient();

	/**
	 * Create the flow used on the server-side for this authentication method.
	 *
	 * @return
	 */
	Mono<AuthServerFlow> createServer();
}
