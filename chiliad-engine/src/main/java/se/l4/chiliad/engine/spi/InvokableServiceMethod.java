package se.l4.chiliad.engine.spi;

public interface InvokableServiceMethod
{
	/**
	 * Get the contract this method follows.
	 *
	 * @return
	 */
	ServiceMethod getContract();
}
