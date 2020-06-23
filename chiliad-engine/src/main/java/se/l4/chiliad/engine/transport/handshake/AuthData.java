package se.l4.chiliad.engine.transport.handshake;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

/**
 * Authentication data part of {@link Auth} flow.
 */
public class AuthData
	implements HandshakeMessage
{
	private final ByteBuf data;

	public AuthData(ByteBuf data)
	{
		this.data = data;
	}

	public ByteBuf getData()
	{
		return data;
	}

	@Override
	public String toString()
	{
		return "AuthData{data=" + ByteBufUtil.hexDump(data) + "}";
	}
}
