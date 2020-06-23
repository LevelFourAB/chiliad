package se.l4.chiliad.engine.transport;

import java.util.List;
import java.util.Optional;

import se.l4.chiliad.engine.auth.AuthMethod;

public class DefaultTransportContext
	implements TransportContext
{
	private final List<AuthMethod> authMethods;

	public DefaultTransportContext(
		List<AuthMethod> authMethods
	)
	{
		this.authMethods = authMethods;
	}

	@Override
	public List<AuthMethod> getAuthMethods()
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
