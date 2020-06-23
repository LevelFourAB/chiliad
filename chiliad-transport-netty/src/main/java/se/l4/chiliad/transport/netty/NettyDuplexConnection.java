package se.l4.chiliad.transport.netty;

import org.reactivestreams.Publisher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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
		return connection.inbound().receiveObject()
			.filter(p -> p instanceof ByteBuf)
			.cast(ByteBuf.class);
	}

	@Override
	public Mono<Void> send(Publisher<ByteBuf> frames)
	{
		return connection.outbound().sendObject(frames).then();
	}

	private ByteBuf encode(ByteBuf frame)
	{
		return frame;
		//return FrameLengthCodec.encode(alloc(), frame.readableBytes(), frame);
	}

	private ByteBuf decode(ByteBuf frame)
	{
		return frame;
		//return FrameLengthCodec.frame(frame).retain();
	}
}
