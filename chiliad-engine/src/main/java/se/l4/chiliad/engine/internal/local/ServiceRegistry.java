package se.l4.chiliad.engine.internal.local;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import se.l4.chiliad.engine.internal.protocol.RemoteServiceEvent;
import se.l4.chiliad.engine.spi.InvokableService;

/**
 * Registry to keep track of a set of services.
 */
public class ServiceRegistry
{
	private final Map<String, InvokableService> byName;
	private final ReplayProcessor<RemoteServiceEvent> serviceChanges;
	private final FluxSink<RemoteServiceEvent> serviceChangesSink;

	public ServiceRegistry()
	{
		byName = new ConcurrentHashMap<>();

		serviceChanges = ReplayProcessor.create(0);
		serviceChangesSink = serviceChanges.sink();
	}

	/**
	 * Add a new invokable service to this registry.
	 *
	 * @param abstraction
	 */
	public void add(InvokableService abstraction)
	{
		if(byName.put(abstraction.getName(), abstraction) == null)
		{
			serviceChangesSink.next(RemoteServiceEvent.available(abstraction.getName()));
		}
	}

	/**
	 * Remove a service from this registry.
	 *
	 * @param name
	 */
	public void remove(String name)
	{
		if(byName.remove(name) != null)
		{
			serviceChangesSink.next(RemoteServiceEvent.unavailable(name));
		}
	}

	public InvokableService get(String name)
	{
		return byName.get(name);
	}

	public boolean has(String name)
	{
		return byName.containsKey(name);
	}

	/**
	 * Get all of the invokable service that are available.
	 *
	 * @return
	 */
	public Flux<InvokableService> current()
	{
		return Flux.defer(() -> Flux.fromIterable(byName.values()));
	}

	/**
	 * Get a flux that will continuously listen to service changes.
	 *
	 * @return
	 */
	public Flux<RemoteServiceEvent> events()
	{
		return serviceChanges;
	}
}
