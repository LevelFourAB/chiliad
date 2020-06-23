package se.l4.chiliad.engine.transport;

import java.util.List;
import java.util.Optional;

import se.l4.chiliad.engine.auth.AuthMethod;

public interface TransportContext
{
	/**
	 * Get all of the authentication methods that are available.
	 *
	 * @return
	 *   all of the authentication methods that are available
	 */
	List<AuthMethod> getAuthMethods();

	/**
	 * Get the specified authentication method.
	 *
	 * @param id
	 * @return
	 */
	Optional<AuthMethod> findMethod(String id);
}
