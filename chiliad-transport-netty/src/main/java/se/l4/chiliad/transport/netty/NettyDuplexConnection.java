package se.l4.chiliad.transport.netty;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.frame.FrameLengthCodec;
import io.rsocket.internal.BaseDuplexConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;

public class NettyDuplexConnection
	extends BaseDuplexConnection
{
	private final Connection connection;

	public NettyDuplexConnection(Connection connection)
	{
		this.connection = connection;

		connection.channel().closeFuture().addListener(f -> this.doOnClose());
	}

	@Override
	public ByteBufAllocator alloc()
	{
		return connection.channel().alloc();
	}

	@Override
	protected void doOnClose()
	{
		if(! connection.isDisposed())
		{
			connection.dispose();
		}
	}

	@Override
	public Flux<ByteBuf> receive()
	{
		return connection.inbound().receive()
			.map(frame -> FrameLengthCodec.frame(frame).retain());
	}

	@Override
	public Mono<Void> send(Publisher<ByteBuf> frames)
	{
		if(frames instanceof Mono)
		{
			return connection.outbound()
				.sendObject(
					((Mono<ByteBuf>) frames)
						.map(this::encode)
				)
				.then();
		}

		return connection.outbound()
			.sendObject(
				Flux.from(frames)
					.map(this::encode)
			).then();
	}

	private ByteBuf encode(ByteBuf frame)
	{
		return FrameLengthCodec.encode(alloc(), frame.readableBytes(), frame);
	}
}
