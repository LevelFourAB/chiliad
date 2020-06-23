package se.l4.chiliad.transport.netty.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import se.l4.chiliad.engine.transport.TransportContext;
import se.l4.chiliad.engine.transport.handshake.Begin;
import se.l4.chiliad.engine.transport.handshake.HandshakeMessage;
import se.l4.chiliad.engine.transport.handshake.ServerHandshaker;

public class ServerHandshakeHandler
	extends ChannelInboundHandlerAdapter
{
	private final ServerHandshaker handshaker;
	private final Runnable onConnected;

	public ServerHandshakeHandler(
		TransportContext context,
		Runnable onConnected
	)
	{
		this.onConnected = onConnected;
		handshaker = new ServerHandshaker(context);
	}

	public void sendInitial(Channel channel)
	{
		handshaker.getInitial()
			.subscribe(msg -> channel.writeAndFlush(msg));
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
		throws Exception
	{
		if(msg instanceof Begin)
		{
			ctx.pipeline().remove(ServerHandshakeHandler.class);
			ConnectionHelper.rewire(ctx);

			onConnected.run();
			ctx.read();
		}
		else
		{
			handshaker.receive((HandshakeMessage) msg)
				.subscribe(nextMsg -> {
					ctx.channel().writeAndFlush(nextMsg);
					ctx.read();
				}, err -> {
					// TODO: What do we do on errors?
					ctx.channel().close();
				});
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx)
		throws Exception
	{
		ctx.read();
	}
}
