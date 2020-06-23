package se.l4.chiliad.engine.internal.transport;

import java.net.URI;
import java.util.function.Supplier;

import io.rsocket.DuplexConnection;
import io.rsocket.transport.ClientTransport;
import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.transport.Transport;
import se.l4.chiliad.engine.transport.TransportContext;

public class RSocketClientTransport
	implements ClientTransport
{
	private final Transport transport;
	private final Supplier<TransportContext> context;

	private final URI uri;

	public RSocketClientTransport(
		Transport transport,
		Supplier<TransportContext> context,
		URI uri
	)
	{
		this.transport = transport;
		this.context = context;
		this.uri = uri;
	}

	@Override
	public Mono<DuplexConnection> connect()
	{
		return transport.connect(context.get(), uri);
	}
}
