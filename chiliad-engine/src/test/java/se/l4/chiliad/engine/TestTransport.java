package se.l4.chiliad.engine;

import java.net.URI;

import io.rsocket.Closeable;
import io.rsocket.DuplexConnection;
import io.rsocket.transport.ServerTransport.ConnectionAcceptor;
import io.rsocket.transport.local.LocalClientTransport;
import io.rsocket.transport.local.LocalServerTransport;
import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.transport.Transport;
import se.l4.chiliad.engine.transport.TransportContext;

public class TestTransport
	implements Transport
{
	private final LocalServerTransport server;

	private TestTransport(String name, boolean bindServer)
	{
		this.server = bindServer ? LocalServerTransport.create(name) : null;
	}

	@Override
	public String getScheme()
	{
		return "local";
	}

	@Override
	public Mono<DuplexConnection> connect(TransportContext encounter, URI uri)
	{
		return LocalClientTransport.create(uri.getHost())
			.connect();
	}

	@Override
	public Mono<Closeable> serve(TransportContext encounter, ConnectionAcceptor acceptor)
	{
		return server == null ? Mono.empty() : server.start(acceptor);
	}

	public static TestTransport server(String name)
	{
		return new TestTransport(name, true);
	}

	public static TestTransport client(String name)
	{
		return new TestTransport(name, false);
	}
}
