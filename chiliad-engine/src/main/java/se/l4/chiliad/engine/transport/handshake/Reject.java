package se.l4.chiliad.engine.transport.handshake;

/**
 * Generic message used to reject something. Can be used to reject a
 * {@link Select}, {@link Auth} or {@link AuthData}.
 */
public class Reject
	implements HandshakeMessage
{
	public static final Reject INSTANCE = new Reject();

	@Override
	public String toString()
	{
		return "Reject{}";
	}
}
