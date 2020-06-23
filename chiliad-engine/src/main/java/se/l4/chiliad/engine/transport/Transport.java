package se.l4.chiliad.engine.transport;

import java.net.URI;

import io.rsocket.Closeable;
import io.rsocket.DuplexConnection;
import io.rsocket.transport.ServerTransport.ConnectionAcceptor;
import reactor.core.publisher.Mono;

/**
 * Transport that can be used to establish a connection between systems.
 */
public interface Transport
{
	/**
	 * Get the {@link URI#getScheme()} this transport supports.
	 *
	 * @return
	 */
	String getScheme();

	/**
	 * Attempt to connect to the given URI.
	 *
	 * @param encounter
	 *   encounter describing the current environment, provides access to things
	 *   such as authentication
	 * @param uri
	 *   URI describing where to connect
	 * @return
	 */
	Mono<DuplexConnection> connect(TransportContext encounter, URI uri);

	/**
	 * Start serving incoming connections over this transport.
	 *
	 * @param encounter
	 *   encounter describing the current environment, provides access to things
	 *   such as authentication
	 * @param acceptor
	 *   object that accepts new connections
	 * @return
	 *   handle that can be used to shutdown the server
	 */
	Mono<Closeable> serve(TransportContext encounter, ConnectionAcceptor acceptor);
}
