package se.l4.chiliad.engine.transport.handshake;

/**
 * Generic message sent as a flag that something previously was sent. Used
 * by the server to indicate that it accepts a {@link Select}. And by the
 * client and server to accept {@link Auth} and {@link AuthData}.
 */
public class Ok
	implements HandshakeMessage
{
	public static final Ok INSTANCE = new Ok();

	@Override
	public String toString()
	{
		return "Ok{}";
	}
}
