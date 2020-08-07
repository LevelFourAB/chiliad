package se.l4.chiliad.engine.internal.spi;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.internal.protocol.Names;
import se.l4.chiliad.engine.spi.InvokableService;
import se.l4.chiliad.engine.spi.InvokableServiceMethod;
import se.l4.chiliad.engine.spi.MethodInvoker;
import se.l4.chiliad.engine.spi.RequestResponseMethod;
import se.l4.chiliad.engine.spi.RequestStreamMethod;
import se.l4.chiliad.engine.spi.ServiceContract;
import se.l4.chiliad.engine.spi.ServiceMethod;

/**
 * Implementation of {@link ServiceContract}.
 */
public class ServiceContractImpl
	implements ServiceContract
{
	private final String name;

	private final ServiceMethod[] methods;

	private ServiceContractImpl(
		String name,
		ServiceMethod[] methods
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
	public ListIterable<ServiceMethod> getMethods()
	{
		return Lists.immutable.of(methods);
	}

	@Override
	public Mono<ServiceMethod> getMethod(String name)
	{
		return Mono.fromSupplier(() -> {
			for(ServiceMethod method : methods)
			{
				if(name.equals(method.getName()))
				{
					return method;
				}
			}

			return null;
		});
	}

	@Override
	public ImplementationBuilder implement()
	{
		return new ImplementationBuilderImpl(Maps.immutable.empty());
	}

	public static Builder create(String name)
	{
		Names.requireValidServiceName(name);

		return new BuilderImpl(name, Lists.immutable.empty());
	}

	private static class BuilderImpl
		implements Builder
	{
		private final String name;
		private final ImmutableList<ServiceMethod> methods;

		public BuilderImpl(String name, ImmutableList<ServiceMethod> methods)
		{
			this.name = name;
			this.methods = methods;
		}

		@Override
		public Builder addMethod(ServiceMethod method)
		{
			return new BuilderImpl(
				name,
				methods.newWith(method)
			);
		}

		@Override
		public ServiceContract build()
		{
			return new ServiceContractImpl(
				name,
				methods.toArray(new ServiceMethod[methods.size()])
			);
		}
	}

	private class ImplementationBuilderImpl
		implements ImplementationBuilder
	{
		private final ImmutableMap<String, InvokableServiceMethod> methods;

		public ImplementationBuilderImpl(ImmutableMap<String, InvokableServiceMethod> methods)
		{
			this.methods = methods;
		}

		private ServiceMethod findLocalMethod(String name)
		{
			for(ServiceMethod method : ServiceContractImpl.this.methods)
			{
				if(name.equals(method.getName()))
				{
					return method;
				}
			}

			return null;
		}

		@Override
		public ImplementationBuilder addMethod(InvokableServiceMethod method)
		{
			ServiceMethod localMethod = findLocalMethod(method.getContract().getName());
			if(localMethod != method.getContract())
			{
				throw new IllegalArgumentException("Tried implementing a method that is not defined by the contract, name was: " + method.getContract().getName());
			}

			// TODO: Protect against duplicates

			return new ImplementationBuilderImpl(
				methods.newWithKeyValue(localMethod.getName(), method)
			);
		}

		@Override
		public ImplementationBuilder requestResponse(String name, MethodInvoker<Mono<? extends Object>> invoker)
		{
			ServiceMethod method = findLocalMethod(name);
			if(! (method instanceof RequestResponseMethod))
			{
				throw new IllegalArgumentException("Tried implementing " + name + ", but method is not a request/response method in the contract");
			}

			RequestResponseMethod typedMethod = (RequestResponseMethod) method;
			return addMethod(
				typedMethod.toInvokable(invoker)
			);
		}

		@Override
		public ImplementationBuilder requestStream(String name, MethodInvoker<Flux<? extends Object>> invoker)
		{
			ServiceMethod method = findLocalMethod(name);
			if(! (method instanceof RequestStreamMethod))
			{
				throw new IllegalArgumentException("Tried implementing " + name + ", but method is not a request/stream method in the contract");
			}

			RequestStreamMethod typedMethod = (RequestStreamMethod) method;
			return addMethod(
				typedMethod.toInvokable(invoker)
			);
		}

		@Override
		public InvokableService build()
		{
			return new InvokableServiceImpl(
				name,
				methods
			);
		}
	}
}
