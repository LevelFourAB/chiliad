package se.l4.chiliad.transport.tcp;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

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
			(address, server) -> new RSocketClientTransport(new TcpTransport(0), this::context, URI.create("chiliad+tcp://127.0.0.1:" + address.getPort())),
			(address) -> new RSocketServerTransport(new TcpTransport(address.getPort()), this::context)
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
		return Duration.ofSeconds(120);
	}

	@Override
	public TransportPair getTransportPair()
	{
		return transportPair;
	}

	@Override
	@Test
	public void requestChannel1() {
		// TODO Auto-generated method stub
		TransportTest.super.requestChannel1();
	}

	@Override
	@Test
	public void requestChannel512()
	{
		TransportTest.super.requestChannel512();
	}

	@Override
	@Test
	public void requestChannel20_000() {
		// TODO Auto-generated method stub
		TransportTest.super.requestChannel20_000();
	}

	@Override
	@Test
	public void metadataPush10() {
		// TODO Auto-generated method stub
		TransportTest.super.metadataPush10();
	}
}
