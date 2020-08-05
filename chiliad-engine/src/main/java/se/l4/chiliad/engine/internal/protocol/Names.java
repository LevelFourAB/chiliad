package se.l4.chiliad.engine.internal.protocol;

import java.util.regex.Pattern;

import se.l4.chiliad.ServiceException;

/**
 * Validation of names used in Chiliad, such as for services and methods.
 */
public class Names
{
	private static final Pattern VALID_METHOD_NAME = Pattern.compile("[a-zA-Z0-9_]+");
	private static final Pattern VALID_SERVICE_NAME = Pattern.compile("[a-zA-Z0-9_:]+");

	private Names()
	{
	}

	public static boolean isValidServiceName(String name)
	{
		return VALID_SERVICE_NAME.matcher(name).matches();
	}

	public static boolean isValidMethodName(String name)
	{
		return VALID_METHOD_NAME.matcher(name).matches();
	}

	public static void requireValidServiceName(String name)
	{
		if(name == null)
		{
			throw new ServiceException("Invalid service name, can not be null");
		}

		if(! isValidServiceName(name))
		{
			throw new ServiceException("Invalid service name, must only contain A-Z, 0-9, underscore (_) or colon (:)");
		}
	}

	public static void requireValidMethodName(String name)
	{
		if(name == null)
		{
			throw new ServiceException("Invalid method name, can not be null");
		}

		if(! isValidMethodName(name))
		{
			throw new ServiceException("Invalid method name, must only contain A-Z, 0-9 or underscore (_)");
		}
	}
}
