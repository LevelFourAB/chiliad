package se.l4.chiliad.transport.netty;

import java.net.URI;
import java.time.Duration;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.rsocket.Closeable;
import io.rsocket.DuplexConnection;
import io.rsocket.frame.FrameLengthCodec;
import io.rsocket.transport.ServerTransport.ConnectionAcceptor;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import se.l4.chiliad.engine.transport.Transport;
import se.l4.chiliad.engine.transport.TransportContext;
import se.l4.chiliad.engine.transport.handshake.Begin;
import se.l4.chiliad.engine.transport.handshake.ClientHandshaker;
import se.l4.chiliad.engine.transport.handshake.HandshakeMessage;
import se.l4.chiliad.engine.transport.handshake.ServerHandshaker;
import se.l4.chiliad.transport.netty.handlers.HandshakeCodec;

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

				c.addHandler("handshake", new HandshakeCodec());

				ClientHandshaker handshaker = new ClientHandshaker(context, c.channel().alloc());
				c.inbound().receiveObject()
					.doOnNext(msg -> System.out.println("C <- " + msg))
					.flatMap(msg -> handshaker.receive((HandshakeMessage) msg))
					.takeUntil(msg -> msg instanceof Begin)
					.subscribe(msg -> {
						System.out.println("C -> " + msg);
						ChannelFuture future = c.channel().writeAndFlush(msg);

						if(msg instanceof Begin)
						{
							future.addListener(l -> {
								logger.debug("Connection established: {}", c);
								System.out.println("C connected");

								c.removeHandler("handshake");
								c.addHandler(new LengthFieldBasedFrameDecoder(
									FrameLengthCodec.FRAME_LENGTH_MASK, 0,
									FrameLengthCodec.FRAME_LENGTH_SIZE, 0, 0
								));
								c.addHandler(new LengthFieldPrepender(FrameLengthCodec.FRAME_LENGTH_SIZE));
								sink.success(new NettyDuplexConnection(c));
							});
						}
					});
			}).subscribe();
		});
	}

	protected abstract Mono<? extends Connection> connect(URI uri, Consumer<? super Connection> consumer);

	@Override
	public Mono<Closeable> serve(
		TransportContext context,
		ConnectionAcceptor acceptor
	)
	{
		return bind(c -> {
			logger.debug("New incoming connection: {}", c);

			c.addHandler("handshake", new HandshakeCodec());

			ServerHandshaker handshaker = new ServerHandshaker(context);
			c.inbound().receiveObject()
				.doOnNext(msg -> {
					System.out.println("S <- " + msg);
					if(msg instanceof Begin)
					{
						logger.debug("Connection established: {}", c);
						System.out.println("S connected");

						c.removeHandler("handshake");
						c.addHandler(new LengthFieldBasedFrameDecoder(
							FrameLengthCodec.FRAME_LENGTH_MASK, 0,
							FrameLengthCodec.FRAME_LENGTH_SIZE, 0, 0
						));
						c.addHandler(new LengthFieldPrepender(FrameLengthCodec.FRAME_LENGTH_SIZE));
						acceptor.apply(new NettyDuplexConnection(c));
					}
				})
				.flatMap(msg -> handshaker.receive((HandshakeMessage) msg))
				.takeUntil(msg -> msg instanceof Begin)
				.subscribe(msg -> {
					if(msg instanceof Begin)
					{

					}
					else
					{
						System.out.println("S -> " + msg);
						c.channel().writeAndFlush(msg);
					}
				});

			handshaker.getInitial()
				.subscribe(msg -> c.channel().writeAndFlush(msg));
		})
			.map(m -> new ServerCloseable2(m));
	}

	protected abstract Mono<? extends DisposableServer> bind(Consumer<? super Connection> onConnection);

	private static class ServerCloseable2
		implements Closeable
	{
		private final DisposableServer server;

		public ServerCloseable2(DisposableServer server)
		{
			this.server = server;
		}

		@Override
		public void dispose()
		{
			server.disposeNow(Duration.ofSeconds(10));
		}

		@Override
		public Mono<Void> onClose()
		{
			return server.onDispose();
		}
	}
}
