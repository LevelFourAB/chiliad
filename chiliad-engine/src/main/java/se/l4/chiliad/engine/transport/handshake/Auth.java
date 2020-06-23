package se.l4.chiliad.engine.transport.handshake;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

/**
 * Message sent by a client to start an authentication flow. Authentication
 * is delegated to {@link se.l4.chiliad.engine.auth.AuthMethod}.
 *
 * <pre>
 * C: AUTH Method Data
 * S: AUTHDATA Data
 * C: AUTHDATA Data
 * S: OK
 * </pre>
 */
public class Auth
	implements HandshakeMessage
{
	private final String method;
	private final ByteBuf data;

	public Auth(String method, ByteBuf data)
	{
		this.method = method;
		this.data = data;
	}

	/**
	 * Get the method being requested.
	 *
	 * @return
	 */
	public String getMethod()
	{
		return method;
	}

	/**
	 * Get the initial data to pass to the authentication method.
	 *
	 * @return
	 */
	public ByteBuf getData()
	{
		return data;
	}

	@Override
	public String toString()
	{
		return "AuthData{method=" + method + ", data=" + ByteBufUtil.hexDump(data) + "}";
	}
}
