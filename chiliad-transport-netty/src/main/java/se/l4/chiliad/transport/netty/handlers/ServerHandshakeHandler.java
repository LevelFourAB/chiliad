package se.l4.chiliad.transport.netty.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import se.l4.chiliad.engine.transport.TransportContext;
import se.l4.chiliad.engine.transport.handshake.Begin;
import se.l4.chiliad.engine.transport.handshake.HandshakeMessage;
import se.l4.chiliad.engine.transport.handshake.ServerHandshaker;

public class ServerHandshakeHandler
	extends ChannelInboundHandlerAdapter
{
	private final ServerHandshaker handshaker;
	private final Runnable connectionReceiver;

	public ServerHandshakeHandler(
		TransportContext context,
		Runnable connectionReceiver
	)
	{
		this.connectionReceiver = connectionReceiver;
		handshaker = new ServerHandshaker(context);
	}

	public void handleInitial(Channel channel)
	{
		handshaker.getInitial()
			.subscribe(msg -> channel.writeAndFlush(msg));
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
		throws Exception
	{
		System.out.println("server " + msg);
		if(msg instanceof Begin)
		{
			// Handshake is complete, need to rewire the pipeline
			ChannelPipeline pipeline = ctx.pipeline();
			pipeline.remove(ServerHandshakeHandler.class);
			pipeline.remove(HandshakeCodec.class);

			// TODO: What do we put here?
			System.out.println("Server connected");

			connectionReceiver.run();
			//acceptor.apply(duplexConnection)
		}
		else
		{
			handshaker.receive((HandshakeMessage) msg)
				.subscribe(nextMsg -> {
					ctx.channel().writeAndFlush(nextMsg);
				}, err -> {
					// TODO: What do we do on errors?
					ctx.channel().close();
				});
		}
	}
}
