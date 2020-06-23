package se.l4.chiliad.transport.tcp;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;

import io.rsocket.test.TransportTest;
import se.l4.chiliad.engine.auth.AnonymousAuth;
import se.l4.chiliad.engine.internal.transport.RSocketClientTransport;
import se.l4.chiliad.engine.internal.transport.RSocketServerTransport;
import se.l4.chiliad.engine.transport.DefaultTransportContext;
import se.l4.chiliad.engine.transport.TransportContext;

public class RSocketTransportTest
	implements TransportTest
{
	private final TcpTransport transport = new TcpTransport(45002);

	private final TransportPair transportPair =
		new TransportPair<>(
			() -> InetSocketAddress.createUnresolved("localhost", 0),
			(address, server) -> new RSocketClientTransport(transport, this::context, URI.create("chiliad+tcp://127.0.0.1:" + 45002)),
			(address) -> new RSocketServerTransport(transport, this::context)
		);

	private TransportContext context()
	{
		return new DefaultTransportContext(
			Collections.singletonList(new AnonymousAuth())
		);
	}

	@Override
	public Duration getTimeout()
	{
		return Duration.ofSeconds(5);
	}

	@Override
	public TransportPair getTransportPair()
	{
		return transportPair;
	}
}
