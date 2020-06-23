package se.l4.chiliad.engine.internal.transport;

import java.util.function.Supplier;

import io.rsocket.Closeable;
import io.rsocket.transport.ServerTransport;
import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.transport.Transport;
import se.l4.chiliad.engine.transport.TransportContext;

/**
 * Adapter used to turn a {@link Transport} into a {@link ServerTransport}
 * used by RSocket.
 */
public class RSocketServerTransport
	implements ServerTransport<Closeable>
{
	private final Transport transport;
	private final Supplier<TransportContext> context;

	public RSocketServerTransport(
		Transport transport,
		Supplier<TransportContext> context
	)
	{
		this.transport = transport;
		this.context = context;
	}

	@Override
	public Mono<Closeable> start(ConnectionAcceptor acceptor)
	{
		return transport.serve(context.get(), acceptor);
	}
}
