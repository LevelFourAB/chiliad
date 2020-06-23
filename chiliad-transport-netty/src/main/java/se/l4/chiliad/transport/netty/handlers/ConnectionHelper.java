package se.l4.chiliad.transport.netty.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.rsocket.frame.FrameLengthCodec;
import reactor.netty.NettyPipeline;

public class ConnectionHelper
{
	private ConnectionHelper()
	{
	}

	public static void rewire(ChannelHandlerContext ctx)
	{
		ChannelPipeline pipeline = ctx.pipeline();
		pipeline.remove(HandshakeCodec.class);

		pipeline.addBefore(NettyPipeline.ReactiveBridge, "lengthDecoder", new LengthFieldBasedFrameDecoder(
			FrameLengthCodec.FRAME_LENGTH_MASK,
			0, FrameLengthCodec.FRAME_LENGTH_SIZE,
			0, 0
		));
	}
}
