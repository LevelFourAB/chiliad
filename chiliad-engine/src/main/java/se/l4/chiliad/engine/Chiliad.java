package se.l4.chiliad.engine;

import java.net.URI;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.l4.chiliad.Service;
import se.l4.chiliad.engine.auth.AuthMethod;
import se.l4.chiliad.engine.internal.ChiliadBuilderImpl;
import se.l4.chiliad.engine.spi.InvokableService;
import se.l4.chiliad.engine.spi.ServiceContract;
import se.l4.chiliad.engine.transport.Transport;

/**
 * Service discovery between systems.
 */
public interface Chiliad
	extends Disposable
{
	/**
	 * Add a new service that will be made available to other nodes.
	 *
	 * @param service
	 */
	<T extends Service> ServiceRegistration addService(T service);

	/**
	 * Add a pre-built {@link InvokableService} to this instance.
	 *
	 * @param service
	 * @return
	 */
	Mono<Disposable> addService(InvokableService service);

	/**
	 * Get the given access service.
	 *
	 * @param <S>
	 * @param service
	 * @return
	 */
	<S extends Service> ServiceBuilder<S> getService(Class<S> service);

	/**
	 * Retrieve a service via name.
	 *
	 * @param name
	 * @return
	 */
	ServiceBuilder<InvokableService> getService(ServiceContract contract);

	/**
	 * Listen to changes to services.
	 *
	 * @return
	 */
	Flux<ServiceEvent> services();

	/**
	 * Connect to an instance via a URI.
	 *
	 * @param uri
	 * @return
	 */
	Mono<Disposable> connect(URI uri);

	/**
	 * Connect to an instance via a URI.
	 *
	 * @param uri
	 * @return
	 */
	Mono<Disposable> connect(String uri);

	/**
	 * Start building a new Chiliad instance.
	 *
	 * <pre>
	 * Chiliad.builder()
	 *   .addAuthMethod(new AnonymousAuth())
	 *   .addTransport(TcpTransport.server(5002))
	 *   .start();
	 * </pre>
	 *
	 * @return
	 */
	public static Builder create()
	{
		return ChiliadBuilderImpl.create();
	}

	interface Builder
	{
		/**
		 * Add a service that should be available directly when the instance
		 * starts.
		 *
		 * @param service
		 *   service to make available locally and to remote instances
		 * @return
		 *   self
		 */
		Builder addService(Service service);

		/**
		 * Add an authentication method that can be used both when connecting
		 * to other instances and when this instance connects to others.
		 *
		 * @param method
		 *   the method that can be used
		 * @return
		 *   self
		 */
		Builder addAuthMethod(AuthMethod method);

		/**
		 * Add a transport engine that can connect to remote instances.
		 *
		 * @param transport
		 *   transport that will start accepting incoming connections when
		 *   this instance starts
		 * @return
		 */
		Builder addTransport(Transport transport);

		/**
		 * Start this instance.
		 *
		 * @return
		 */
		Mono<Chiliad> start();
	}
}
