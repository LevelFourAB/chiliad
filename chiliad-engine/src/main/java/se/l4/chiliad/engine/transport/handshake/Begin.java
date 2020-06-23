package se.l4.chiliad.engine.transport.handshake;

/**
 * Message that indicates that the setup is complete and that requests can now
 * start. Sent from the client to the server after {@link Auth} completes.
 *
 * <p>
 * When this message is sent the client must be in a state where it can handle
 * receiving {@link io.rsocket.Payload}s.
 */
public class Begin
	implements HandshakeMessage
{
	public static final Begin INSTANCE = new Begin();

	@Override
	public String toString()
	{
		return "Begin{}";
	}
}
