package se.l4.chiliad.engine.internal.spi;

import org.eclipse.collections.api.map.ImmutableMap;

import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.spi.InvokableService;
import se.l4.chiliad.engine.spi.InvokableServiceMethod;

/**
 * Implementation of {@link InvokableService}.
 */
public class InvokableServiceImpl
	implements InvokableService
{
	private final String name;
	private final ImmutableMap<String, InvokableServiceMethod> methods;

	public InvokableServiceImpl(
		String name,
		ImmutableMap<String, InvokableServiceMethod> methods
	)
	{
		this.name = name;
		this.methods = methods;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public Mono<InvokableServiceMethod> getMethod(String name)
	{
		return Mono.justOrEmpty(methods.get(name));
	}
}
