package se.l4.chiliad;

/**
 * Exception thrown by Chiliad services if they can not be invoked.
 */
public class ServiceException
	extends RuntimeException
{
	public ServiceException()
	{
	}

	public ServiceException(String message)
	{
		super(message);
	}

	public ServiceException(Throwable cause)
	{
		super(cause);
	}

	public ServiceException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
