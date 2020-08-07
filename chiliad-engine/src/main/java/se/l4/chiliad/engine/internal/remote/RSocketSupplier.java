package se.l4.chiliad.engine.internal.remote;

import io.rsocket.RSocket;

/**
 * Abstract used to pick out a {@link RSocket} to use for an invocation.
 */
public interface RSocketSupplier
{
	RSocket get();
}
