package se.l4.chiliad.engine.transport;

import java.util.Optional;

import org.eclipse.collections.api.list.ImmutableList;

import se.l4.chiliad.engine.auth.AuthMethod;

public interface TransportContext
{
	/**
	 * Get all of the authentication methods that are available.
	 *
	 * @return
	 *   all of the authentication methods that are available
	 */
	ImmutableList<AuthMethod> getAuthMethods();

	/**
	 * Get the specified authentication method.
	 *
	 * @param id
	 * @return
	 */
	Optional<AuthMethod> findMethod(String id);
}
