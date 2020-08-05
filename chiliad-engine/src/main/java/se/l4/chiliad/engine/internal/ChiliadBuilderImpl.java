package se.l4.chiliad.engine.internal;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.collector.Collectors2;
import org.eclipse.collections.impl.factory.Lists;

import io.rsocket.Closeable;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.chiliad.Service;
import se.l4.chiliad.engine.Chiliad;
import se.l4.chiliad.engine.Chiliad.Builder;
import se.l4.chiliad.engine.auth.AuthMethod;
import se.l4.chiliad.engine.internal.local.LocalServicesManager;
import se.l4.chiliad.engine.internal.remote.RemoteServicesManager;
import se.l4.chiliad.engine.internal.transport.RSocketServerTransport;
import se.l4.chiliad.engine.transport.DefaultTransportContext;
import se.l4.chiliad.engine.transport.Transport;
import se.l4.chiliad.engine.transport.TransportContext;

public class ChiliadBuilderImpl
	implements Chiliad.Builder
{
	private final ImmutableList<AuthMethod> authMethods;
	private final ImmutableSet<Transport> transports;

	private ChiliadBuilderImpl(
		ImmutableList<AuthMethod> authMethods,
		ImmutableSet<Transport> transports
	)
	{
		this.authMethods = authMethods;
		this.transports = transports;
	}

	@Override
	public Builder addAuthMethod(AuthMethod method)
	{
		return new ChiliadBuilderImpl(
			authMethods.newWith(method),
			transports
		);
	}

	@Override
	public Builder addService(Service service)
	{
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public Builder addTransport(Transport transport)
	{
		return new ChiliadBuilderImpl(
			authMethods,
			transports.newWith(transport)
		);
	}

	@Override
	public Mono<Chiliad> start()
	{
		return Mono.defer(() -> {
			LocalServicesManager localServices = new LocalServicesManager();
			RemoteServicesManager remoteServices = new RemoteServicesManager();
			TransportContext context = new DefaultTransportContext(authMethods);

			return bindServers(localServices, remoteServices, context)
				.collect(Collectors2.toImmutableSet())
				.map(servers -> new ChiliadImpl(
					localServices,
					remoteServices,
					context,
					transports,
					servers
				));
		});
	}

	/**
	 * Bind all the servers as defined by the transports.
	 *
	 * @param localServices
	 * @param context
	 * @return
	 */
	private Flux<Closeable> bindServers(
		LocalServicesManager localServices,
		RemoteServicesManager remoteServices,
		TransportContext context
	)
	{
		SocketAcceptor acceptor = (setup, sendingRSocket) -> {
			remoteServices.registerRemote(sendingRSocket);
			return Mono.just(localServices.incomingHandler());
		};

		return Flux.fromIterable(transports)
			.flatMap(transport -> RSocketServer.create(acceptor)
				.payloadDecoder(PayloadDecoder.ZERO_COPY)
				.bind(new RSocketServerTransport(transport, () -> context))
			);
	}

	public static Chiliad.Builder create()
	{
		return new ChiliadBuilderImpl(
			Lists.immutable.empty(),
			Sets.immutable.empty()
		);
	}
}
