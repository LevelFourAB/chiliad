package se.l4.chiliad.engine.transport.handshake;

import java.util.Arrays;

/**
 * Message used to select some capabilities from a {@link Hello} and to
 * continue the handshake.
 *
 * Part of the initial connection:
 *
 * <pre>
 * S: HELLO Capability[]
 * C: SELECT Capability[]
 * S: OK
 * </pre>
 */
public class Select
	implements HandshakeMessage
{
	private final String[] capabilities;

	public Select(String[] capabilities)
	{
		this.capabilities = capabilities;
	}

	public String[] getCapabilities()
	{
		return capabilities;
	}

	@Override
	public String toString()
	{
		return "Select{capabilities=" + Arrays.toString(capabilities) + "}";
	}
}
