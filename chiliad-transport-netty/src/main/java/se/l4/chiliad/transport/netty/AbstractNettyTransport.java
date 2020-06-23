package se.l4.chiliad.transport.netty;

import java.net.URI;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.rsocket.Closeable;
import io.rsocket.DuplexConnection;
import io.rsocket.transport.ServerTransport.ConnectionAcceptor;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import se.l4.chiliad.engine.transport.Transport;
import se.l4.chiliad.engine.transport.TransportContext;
import se.l4.chiliad.transport.netty.handlers.ClientHandshakeHandler;
import se.l4.chiliad.transport.netty.handlers.HandshakeCodec;
import se.l4.chiliad.transport.netty.handlers.ServerHandshakeHandler;

public abstract class AbstractNettyTransport
	implements Transport
{
	protected final Logger logger;

	public AbstractNettyTransport()
	{
		logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public Mono<DuplexConnection> connect(TransportContext context, URI uri)
	{
		return Mono.create(sink -> {
			connect(uri, c -> {
				logger.debug("Connection established: {}", c);

				c.addHandlerLast("handshake", new HandshakeCodec());
				c.addHandlerLast("handler", new ClientHandshakeHandler(context, c.channel().alloc(), () -> {
					sink.success(new NettyDuplexConnection(c));
				}));

				c.channel().read();
			})
			.subscribe();
		});
	}

	protected abstract Mono<? extends Connection> connect(URI uri, Consumer<? super Connection> doOnConnected);

	@Override
	public Mono<Closeable> serve(
		TransportContext context,
		ConnectionAcceptor acceptor
	)
	{
		return bind(c -> {
			logger.debug("New incoming connection: {}", c);

			c.addHandlerLast("handshake", new HandshakeCodec());

			ServerHandshakeHandler handler = new ServerHandshakeHandler(context,() -> {
				acceptor.apply(new NettyDuplexConnection(c))
					.then(Mono.<Void>never())
					.subscribe(c.disposeSubscriber());
			});

			c.addHandlerLast("handler", handler);

			c.channel().read();

			handler.sendInitial(c.channel());
		})
			.map(m -> new ServerCloseable(m));
	}

	protected abstract Mono<? extends DisposableServer> bind(Consumer<? super Connection> onConnection);

	private static class ServerCloseable
		implements Closeable
	{
		private final DisposableServer server;

		public ServerCloseable(DisposableServer server)
		{
			this.server = server;
		}

		@Override
		public void dispose()
		{
			server.dispose();
		}

		@Override
		public boolean isDisposed()
		{
			return server.isDisposed();
		}

		@Override
		public Mono<Void> onClose()
		{
			return server.onDispose();
		}
	}
}
