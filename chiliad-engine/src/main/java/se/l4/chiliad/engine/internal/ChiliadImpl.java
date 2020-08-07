package se.l4.chiliad.engine.internal;

import java.net.URI;

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
import se.l4.chiliad.RemoteName;
import se.l4.chiliad.Service;
import se.l4.chiliad.ServiceException;
import se.l4.chiliad.engine.Chiliad;
import se.l4.chiliad.engine.ServiceBuilder;
import se.l4.chiliad.engine.ServiceEvent;
import se.l4.chiliad.engine.ServiceRegistration;
import se.l4.chiliad.engine.internal.local.LocalServicesManager;
import se.l4.chiliad.engine.internal.reflection.LocalServiceGenerator;
import se.l4.chiliad.engine.internal.reflection.RemoteServiceGenerator;
import se.l4.chiliad.engine.internal.reflection.ServiceContractGenerator;
import se.l4.chiliad.engine.internal.remote.RemoteServicesManager;
import se.l4.chiliad.engine.internal.transport.RSocketClientTransport;
import se.l4.chiliad.engine.spi.InvokableService;
import se.l4.chiliad.engine.spi.ServiceContract;
import se.l4.chiliad.engine.transport.Transport;
import se.l4.chiliad.engine.transport.TransportContext;
import se.l4.chiliad.engine.transport.TransportException;
import se.l4.exobytes.Serializers;
import se.l4.ylem.types.reflect.TypeRef;
import se.l4.ylem.types.reflect.Types;

public class ChiliadImpl
	implements Chiliad
{
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

	/**
	 * Manager used to track the local services that are exported to peers.
	 */
	private final LocalServicesManager localServices;

	/**
	 * Manager used to track remote services that are imported from peers.
	 */
	private final RemoteServicesManager remoteServices;

	/**
	 * Generator that is used to turn instances into {@link InvokableService}s.
	 */
	private final LocalServiceGenerator localServiceGenerator;

	/**
	 * Generator that can generate an implementation of an interface that
	 * calls a remote service.
	 */
	private final RemoteServiceGenerator remoteServiceGenerator;

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

		// TODO: Serializers should be passed to ChiliadImpl
		ServiceContractGenerator generator = new ServiceContractGenerator(Serializers.create().build());
		localServiceGenerator = new LocalServiceGenerator(generator);
		remoteServiceGenerator = new RemoteServiceGenerator(generator);
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
		return new ServiceRegistration()
		{
			@Override
			public Mono<Disposable> register()
			{
				return Mono.fromSupplier(() -> localServiceGenerator.generate(service))
					.flatMap(ChiliadImpl.this::addService);
			}
		};
	}

	@Override
	public Mono<Disposable> addService(InvokableService service)
	{
		return localServices.register(service);
	}

	@Override
	public <S extends Service> ServiceBuilder<S> createRemoteService(Class<S> service)
	{
		return new ServiceBuilderImpl<>(service);
	}

	@Override
	public InvokableService getRemoteService(ServiceContract contract)
	{
		return remoteServices.createRemoteService(contract);
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
		private final Class<S> service;

		public ServiceBuilderImpl(Class<S> service)
		{
			this.service = service;
		}

		@Override
		public Mono<S> whenAvailable()
		{
			return build()
				.delayUntil(service -> services()
					.filter(ServiceEvent.isAvailable("test"))
					.single()
				);
		}

		@Override
		public Mono<S> build()
		{
			return Mono.fromSupplier(() -> {
				TypeRef type = Types.reference(service);
				String name = type.findAnnotation(RemoteName.class)
					.orElseThrow(() -> new ServiceException("No name defined for " + type.toTypeDescription()))
					.value();

				return remoteServiceGenerator.toService(type, name, remoteServices.createRandomSupplier(name));
			});
		}
	}
}
