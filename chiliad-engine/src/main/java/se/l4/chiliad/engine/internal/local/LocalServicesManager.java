package se.l4.chiliad.engine.internal.local;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.rsocket.RSocket;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.internal.protocol.CoreService;
import se.l4.chiliad.engine.internal.protocol.RemoteServiceEvent;
import se.l4.chiliad.engine.spi.InvokableService;
import se.l4.chiliad.engine.spi.ServiceContract;

/**
 * Manager for all the services that have been registered locally. Provides
 * the core service ({@code chiliad:core}) and provides an
 * {@link #incomingHandler() RSocket that handles incoming calls}.
 */
public class LocalServicesManager
{
	private final ServiceRegistry registry;
	private final Lock lock;

	private final IncomingMessageHandler incomingHandler;

	public LocalServicesManager()
	{
		this.registry = new ServiceRegistry();
		this.lock = new ReentrantLock();

		InvokableService coreService = CoreService.SERVICE.implement()
			.requestStream("services", args -> currentAndFutureServiceEvents())
			.build();

		this.registry.add(coreService);

		this.incomingHandler = new IncomingMessageHandler(registry);
	}

	/**
	 * Get a {@link RSocket} that should be used for handling of incoming
	 * requests.
	 *
	 * @return
	 */
	public RSocket incomingHandler()
	{
		return incomingHandler;
	}

	/**
	 * Register a local implementation of a {@link ServiceContract}.
	 *
	 * @param service
	 * @return
	 */
	public Mono<Disposable> register(InvokableService service)
	{
		return Mono.fromCallable(() -> {
			lock.lock();
			try
			{
				// TODO: Protect against duplicate services with the same name
				registry.add(service);

				return () -> this.unregister(service.getName());
			}
			finally
			{
				lock.unlock();
			}
		});
	}

	/**
	 * Unregister a previously registered service.
	 *
	 * @param name
	 */
	private void unregister(String name)
	{
		lock.lock();
		try
		{
			registry.remove(name);
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Get all of the current services as they were being registered and also
	 * start listening for changes to them.
	 *
	 * @return
	 */
	Flux<RemoteServiceEvent> currentAndFutureServiceEvents()
	{
		return registry.current()
			.filter(s -> ! "chiliad:core".equals(s.getName()))
			.map(s -> RemoteServiceEvent.available(s.getName()))
			.concatWith(registry.events());
	}
}
