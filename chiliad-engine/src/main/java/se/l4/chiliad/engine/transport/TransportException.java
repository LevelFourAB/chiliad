package se.l4.chiliad.engine.transport;

public class TransportException
	extends RuntimeException
{
	public TransportException()
	{
	}

	public TransportException(String message)
	{
		super(message);
	}

	public TransportException(Throwable cause)
	{
		super(cause);
	}

	public TransportException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
