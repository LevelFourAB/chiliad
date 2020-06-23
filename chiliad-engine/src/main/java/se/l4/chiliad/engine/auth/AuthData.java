package se.l4.chiliad.engine.auth;

import io.netty.buffer.ByteBuf;

/**
 * Authentication data has been returned, should be sent to the server or
 * client.
 */
public class AuthData
	implements AuthReply
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
}
