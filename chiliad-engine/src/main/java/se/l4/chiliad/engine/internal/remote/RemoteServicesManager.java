package se.l4.chiliad.engine.internal.remote;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

import io.rsocket.RSocket;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import se.l4.chiliad.engine.ServiceEvent;
import se.l4.chiliad.engine.internal.protocol.CoreService;
import se.l4.chiliad.engine.internal.protocol.RemoteServiceEvent;
import se.l4.chiliad.engine.spi.InvokableService;
import se.l4.chiliad.engine.spi.InvokableServiceMethod;
import se.l4.chiliad.engine.spi.RequestResponseMethod;
import se.l4.chiliad.engine.spi.RequestStreamMethod;
import se.l4.chiliad.engine.spi.ServiceContract;
import se.l4.chiliad.engine.spi.ServiceMethod;

/**
 * Manager that keeps track of remote services.
 */
public class RemoteServicesManager
{
	private static final Object[] EMPTY_ARGS = new Object[0];

	private final Lock lock;

	private final MutableMap<RSocket, Peer> peers;
	private final MutableMap<String, RemoteService> remoteServices;

	private final ReplayProcessor<ServiceEvent> remoteServiceChanges;
	private final FluxSink<ServiceEvent> remoteServiceChangesSink;

	public RemoteServicesManager()
	{
		peers = new ConcurrentHashMap<>();
		remoteServices = new ConcurrentHashMap<>();

		lock = new ReentrantLock();

		remoteServiceChanges = ReplayProcessor.create(0);
		remoteServiceChangesSink = remoteServiceChanges.sink();
	}

	public Flux<ServiceEvent> events()
	{
		return remoteServiceChanges;
	}

	/**
	 * Register a remote socket and start tracking what services are available
	 * for the instance.
	 *
	 * @param socket
	 */
	public void registerRemote(RSocket socket)
	{
		// Start tracking the peer
		peers.put(socket, new Peer(socket, Sets.immutable.empty()));

		socket.onClose()
			.subscribe(o -> {
				System.out.println("Socket is no longer available");

				// TODO: Remove peer from all services
				peers.remove(socket);
			});

		// Request a stream of service events
		RemoteInvokers.requestStream(CoreService.NAME, CoreService.SERVICES_METHOD, () -> socket)
			.invoke(EMPTY_ARGS)
			.subscribe(o -> {
				RemoteServiceEvent event = (RemoteServiceEvent) o;
				switch(event.getType())
				{
					case AVAILABLE:
						registerService(socket, event.getName());
						break;
					case UNAVAILABLE:
						unregisterService(socket, event.getName());
						break;
				}
			});
	}

	/**
	 * Take care of a service now being available via the given peer.
	 *
	 * @param peer
	 * @param name
	 */
	private void registerService(RSocket peer, String name)
	{
		lock.lock();
		try
		{
			peers.put(peer, peers.get(peer).withService(name));

			RemoteService service = remoteServices.getIfAbsentPut(name, RemoteService::new);
			if(service.addSocket(peer))
			{
				remoteServiceChangesSink.next(new ServiceEvent(ServiceEvent.Type.AVAILABLE, name));
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Take care of a service now being available via the given peer.
	 *
	 * @param peer
	 * @param name
	 */
	private void unregisterService(RSocket peer, String name)
	{
		lock.lock();
		try
		{
			peers.put(peer, peers.get(peer).withoutService(name));

			RemoteService service = remoteServices.getIfAbsentPut(name, RemoteService::new);
			if(service.removeSocket(peer))
			{
				remoteServiceChangesSink.next(new ServiceEvent(ServiceEvent.Type.UNAVAILABLE, name));
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	public InvokableService createRemoteService(ServiceContract contract)
	{
		String name = contract.getName();
		RemoteService service = remoteServices.getIfAbsentPut(name, RemoteService::new);

		ServiceContract.ImplementationBuilder builder = contract.implement();
		for(ServiceMethod m : contract.getMethods())
		{
			InvokableServiceMethod invokable;
			if(m instanceof RequestResponseMethod)
			{
				invokable = ((RequestResponseMethod) m).toInvokable(
					RemoteInvokers.requestResponse(name, (RequestResponseMethod) m, service::pickRandom)
				);
			}
			else if(m instanceof RequestStreamMethod)
			{
				invokable = ((RequestStreamMethod) m).toInvokable(
					RemoteInvokers.requestStream(name, (RequestStreamMethod) m, service::pickRandom)
				);
			}
			else
			{
				throw new IllegalArgumentException();
			}

			builder = builder.addMethod(invokable);
		}

		return builder.build();
	}

	public RSocketSupplier createRandomSupplier(String name)
	{
		RemoteService service = remoteServices.getIfAbsentPut(name, RemoteService::new);
		return service::pickRandom;
	}
}
