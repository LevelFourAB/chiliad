package se.l4.chiliad.engine.transport.handshake;

import java.util.Arrays;

/**
 * Hello sent from the server when a connection is established. Contains
 * information about the capabilities a server has that can be selected.
 *
 * Part of the initial connection:
 *
 * <pre>
 * S: HELLO Capability[]
 * C: SELECT Capability[]
 * S: OK
 * </pre>
 */
public class Hello
	implements HandshakeMessage
{
	/**
	 * Capabilities that the server has.
	 */
	private final String[] capabilities;

	public Hello(String[] capabilities)
	{
		this.capabilities = capabilities;
	}

	/**
	 * Get the capabilities that the server is advertising.
	 *
	 * @return
	 */
	public String[] getCapabilities()
	{
		return capabilities;
	}

	@Override
	public String toString()
	{
		return "Hello{capabilities=" + Arrays.toString(capabilities) + "}";
	}
}
