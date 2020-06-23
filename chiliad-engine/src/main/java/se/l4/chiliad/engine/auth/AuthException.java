package se.l4.chiliad.engine.auth;

public class AuthException
	extends RuntimeException
{
	public AuthException()
	{
	}

	public AuthException(String message)
	{
		super(message);
	}

	public AuthException(Throwable cause)
	{
		super(cause);
	}

	public AuthException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
