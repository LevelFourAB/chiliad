package se.l4.chiliad.engine.internal;

import java.net.URI;
import java.util.function.Function;

import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import io.rsocket.Closeable;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketConnector;
import io.rsocket.frame.decoder.PayloadDecoder;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.chiliad.Service;
import se.l4.chiliad.engine.Chiliad;
import se.l4.chiliad.engine.ServiceBuilder;
import se.l4.chiliad.engine.ServiceEvent;
import se.l4.chiliad.engine.ServiceRegistration;
import se.l4.chiliad.engine.internal.local.LocalServicesManager;
import se.l4.chiliad.engine.internal.remote.RemoteServicesManager;
import se.l4.chiliad.engine.internal.transport.RSocketClientTransport;
import se.l4.chiliad.engine.spi.InvokableService;
import se.l4.chiliad.engine.spi.ServiceContract;
import se.l4.chiliad.engine.transport.Transport;
import se.l4.chiliad.engine.transport.TransportContext;
import se.l4.chiliad.engine.transport.TransportException;

public class ChiliadImpl
	implements Chiliad
{
	private final LocalServicesManager localServices;
	private final RemoteServicesManager remoteServices;

	/**
	 * Context used for transports.
	 */
	private final TransportContext transportContext;

	/**
	 * Transports that can be used to connect to other instances.
	 */
	private final ImmutableSet<Transport> transports;

	private final ImmutableSet<Closeable> transportServers;

	private final MutableSet<RSocket> clients;

	ChiliadImpl(
		LocalServicesManager localServices,
		RemoteServicesManager remoteServices,
		TransportContext transportContext,
		ImmutableSet<Transport> transports,
		ImmutableSet<Closeable> transportServers
	)
	{
		this.localServices = localServices;
		this.remoteServices = remoteServices;
		this.transportContext = transportContext;
		this.transports = transports;
		this.transportServers = transportServers;

		clients = new UnifiedSet<>();
	}

	@Override
	public Mono<Disposable> connect(String uri)
	{
		return connect(URI.create(uri));
	}

	@Override
	public Mono<Disposable> connect(URI uri)
	{
		return Mono.defer(() -> {
			String scheme = uri.getScheme();
			for(Transport transport : transports)
			{
				if(scheme.equals(transport.getScheme()))
				{
					return RSocketConnector.create()
						.payloadDecoder(PayloadDecoder.ZERO_COPY)
						.acceptor(SocketAcceptor.with(localServices.incomingHandler()))
						.connect(new RSocketClientTransport(transport, () -> transportContext, uri))
						.doOnNext(socket -> {
							clients.add(socket);
							remoteServices.registerRemote(socket);
						});
				}
			}

			return Mono.error(new TransportException("No transport available for " + uri));
		});
	}

	@Override
	public <T extends Service> ServiceRegistration addService(T service)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Mono<Disposable> addService(InvokableService service)
	{
		return localServices.register(service);
	}

	@Override
	public <S extends Service> ServiceBuilder<S> getService(Class<S> service)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceBuilder<InvokableService> getService(ServiceContract contract)
	{
		return new ServiceBuilderImpl<>(
			remoteServices.createRemoteService(contract),
			s -> s
		);
	}

	@Override
	public Flux<ServiceEvent> services()
	{
		return remoteServices.events();
	}

	@Override
	public void dispose()
	{
		for(Disposable d : transportServers)
		{
			d.dispose();
		}

		for(Disposable d : clients)
		{
			d.dispose();
		}
	}

	private class ServiceBuilderImpl<S>
		implements ServiceBuilder<S>
	{
		private final InvokableService service;
		private final Function<InvokableService, S> factory;

		public ServiceBuilderImpl(
			InvokableService service,
			Function<InvokableService, S> factory
		)
		{
			this.service = service;
			this.factory = factory;
		}

		@Override
		public ServiceBuilder<S> withFactory(Function<InvokableService, S> factory)
		{
			return new ServiceBuilderImpl<>(service, factory);
		}

		@Override
		public S build()
		{
			return factory.apply(service);
		}
	}
}
