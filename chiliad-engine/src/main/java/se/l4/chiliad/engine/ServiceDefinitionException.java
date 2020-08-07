package se.l4.chiliad.engine;

import se.l4.chiliad.ServiceException;
import se.l4.chiliad.engine.spi.InvokableService;
import se.l4.chiliad.engine.spi.ServiceContract;

/**
 * Exception that indicates an issue with a definition of service, either via
 * reflection or during manual creation of {@link ServiceContract} and
 * {@link InvokableService}.
 */
public class ServiceDefinitionException
	extends ServiceException
{
	public ServiceDefinitionException(String message)
	{
		super(message);
	}

	public ServiceDefinitionException(Throwable cause)
	{
		super(cause);
	}

	public ServiceDefinitionException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ServiceDefinitionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
