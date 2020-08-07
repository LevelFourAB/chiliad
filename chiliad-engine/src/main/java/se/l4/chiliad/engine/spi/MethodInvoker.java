package se.l4.chiliad.engine.spi;

@FunctionalInterface
public interface MethodInvoker<T>
{
	T invoke(Object[] args);
}
