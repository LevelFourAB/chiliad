package se.l4.chiliad.transport.tcp;

import java.net.URI;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;
import se.l4.chiliad.transport.netty.AbstractNettyTransport;

/**
 * Transport that connects to other instances using TCP.
 */
public class TCPTransport
	extends AbstractNettyTransport
{
	private final int port;

	TCPTransport(int port)
	{
		this.port = port;
	}

	@Override
	public String getScheme()
	{
		return "chiliad+tcp";
	}

	@Override
	protected Mono<? extends DisposableServer> bind(Consumer<? super Connection> onConnection)
	{
		// If there is no port defined return an empty mono
		if(port <= 0) return Mono.empty();

		return TcpServer.create()
			.doOnBind(b -> logger.info("Starting server at port " + port))
			.doOnConnection(onConnection)
			.port(port)
			//.wiretap("server", LogLevel.INFO)
			.bind();
	}

	@Override
	protected Mono<? extends Connection> connect(URI uri, Consumer<? super Connection> consumer)
	{
		return TcpClient.create()
			.doOnConnected(consumer)
			.host(uri.getHost())
			.port(uri.getPort())
			//.wiretap("client", LogLevel.INFO)
			.connect();
	}

	/**
	 * Start creating a new instance of this transport.
	 *
	 * @return
	 */
	public static Builder create()
	{
		return new Builder(-1);
	}

	public static class Builder
	{
		private final int port;

		public Builder(int port)
		{
			this.port = port;
		}

		/**
		 * Setup that this transport should start a server on the given port.
		 *
		 * @param port
		 * @return
		 */
		public Builder withPort(int port)
		{
			return new Builder(port);
		}

		/**
		 * Build the instance.
		 *
		 * @return
		 */
		public TCPTransport build()
		{
			return new TCPTransport(port);
		}
	}
}
