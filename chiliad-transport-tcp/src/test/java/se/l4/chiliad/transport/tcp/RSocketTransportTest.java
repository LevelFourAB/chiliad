package se.l4.chiliad.transport.tcp;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.collections.api.factory.Lists;

import io.rsocket.test.TransportTest;
import se.l4.chiliad.engine.auth.AnonymousAuth;
import se.l4.chiliad.engine.internal.transport.RSocketClientTransport;
import se.l4.chiliad.engine.internal.transport.RSocketServerTransport;
import se.l4.chiliad.engine.transport.DefaultTransportContext;
import se.l4.chiliad.engine.transport.TransportContext;

public class RSocketTransportTest
	implements TransportTest
{
	private final TransportPair transportPair =
		new TransportPair<>(
			() -> InetSocketAddress.createUnresolved("localhost", ThreadLocalRandom.current().nextInt(11000, 14000)),
			(address, server) -> new RSocketClientTransport(new TCPTransport(0), this::context, URI.create("chiliad+tcp://127.0.0.1:" + address.getPort())),
			(address) -> new RSocketServerTransport(new TCPTransport(address.getPort()), this::context)
		);

	private TransportContext context()
	{
		return new DefaultTransportContext(
			Lists.immutable.of(new AnonymousAuth())
		);
	}

	@Override
	public Duration getTimeout()
	{
		return Duration.ofSeconds(120);
	}

	@Override
	public TransportPair getTransportPair()
	{
		return transportPair;
	}
}
