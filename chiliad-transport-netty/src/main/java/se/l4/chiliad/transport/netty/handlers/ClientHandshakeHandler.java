package se.l4.chiliad.transport.netty.handlers;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import se.l4.chiliad.engine.transport.TransportContext;
import se.l4.chiliad.engine.transport.handshake.Begin;
import se.l4.chiliad.engine.transport.handshake.ClientHandshaker;
import se.l4.chiliad.engine.transport.handshake.HandshakeMessage;

public class ClientHandshakeHandler
	extends ChannelInboundHandlerAdapter
{
	private final ClientHandshaker handshaker;

	public ClientHandshakeHandler(TransportContext context, ByteBufAllocator allocator)
	{
		handshaker = new ClientHandshaker(context, allocator);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
		throws Exception
	{
		handshaker.receive((HandshakeMessage) msg)
			.subscribe(nextMsg -> {
				ctx.channel().writeAndFlush(nextMsg);

				if(nextMsg instanceof Begin)
				{
					ChannelPipeline pipeline = ctx.pipeline();
					pipeline.remove(ClientHandshakeHandler.class);
					pipeline.remove(HandshakeCodec.class);
				}
			}, err -> {
				// TODO: What do we do on errors?
				ctx.channel().close();
			});
}
}
