package se.l4.chiliad.engine.transport;

import java.util.Optional;

import org.eclipse.collections.api.list.ImmutableList;

import se.l4.chiliad.engine.auth.AuthMethod;

public class DefaultTransportContext
	implements TransportContext
{
	private final ImmutableList<AuthMethod> authMethods;

	public DefaultTransportContext(
		ImmutableList<AuthMethod> authMethods
	)
	{
		this.authMethods = authMethods;
	}

	@Override
	public ImmutableList<AuthMethod> getAuthMethods()
	{
		return authMethods;
	}

	@Override
	public Optional<AuthMethod> findMethod(String id)
	{
		for(AuthMethod m : authMethods)
		{
			if(m.getId().equals(id))
			{
				return Optional.of(m);
			}
		}

		return Optional.empty();
	}
}
