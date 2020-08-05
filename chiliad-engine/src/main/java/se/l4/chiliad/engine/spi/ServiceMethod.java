package se.l4.chiliad.engine.spi;

/**
 * Information about a method in a {@link ServiceContract}.
 */
public interface ServiceMethod
{
	/**
	 * Get the name of the method.
	 *
	 * @return
	 */
	String getName();
}
