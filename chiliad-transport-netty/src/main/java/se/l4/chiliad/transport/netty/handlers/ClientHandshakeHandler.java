package se.l4.chiliad.transport.netty.handlers;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import se.l4.chiliad.engine.transport.TransportContext;
import se.l4.chiliad.engine.transport.handshake.Begin;
import se.l4.chiliad.engine.transport.handshake.ClientHandshaker;
import se.l4.chiliad.engine.transport.handshake.HandshakeMessage;

public class ClientHandshakeHandler
	extends ChannelInboundHandlerAdapter
{
	private final ClientHandshaker handshaker;
	private final Runnable onConnected;

	public ClientHandshakeHandler(
		TransportContext context,
		ByteBufAllocator allocator,
		Runnable onConnected
	)
	{
		handshaker = new ClientHandshaker(context, allocator);

		this.onConnected = onConnected;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
		throws Exception
	{
		handshaker.receive((HandshakeMessage) msg)
			.subscribe(nextMsg -> {
				ChannelFuture future = ctx.channel().writeAndFlush(nextMsg);

				if(nextMsg instanceof Begin)
				{
					future.addListener(f -> {
						if(f.isSuccess())
						{
							ctx.pipeline().remove(ClientHandshakeHandler.class);
							ConnectionHelper.rewire(ctx);

							onConnected.run();
						}
						else
						{
							ctx.channel().close();
						}
					});
				}

				ctx.read();
			}, err -> {
				// TODO: What do we do on errors?
				ctx.channel().close();
			});
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx)
		throws Exception
	{
		ctx.read();
	}
}
