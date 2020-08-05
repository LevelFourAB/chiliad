package se.l4.chiliad.engine.internal.remote;

import org.eclipse.collections.api.set.ImmutableSet;

import io.rsocket.RSocket;

/**
 * Things that are tracked for a peer.
 */
public class Peer
{
	private final RSocket socket;
	private final ImmutableSet<String> services;

	public Peer(
		RSocket socket,
		ImmutableSet<String> services
	)
	{
		this.socket = socket;
		this.services = services;
	}

	public RSocket getSocket()
	{
		return socket;
	}

	public ImmutableSet<String> getServices()
	{
		return services;
	}

	public Peer withService(String service)
	{
		return new Peer(socket, services.newWith(service));
	}

	public Peer withoutService(String service)
	{
		return new Peer(socket, services.newWithout(service));
	}
}
