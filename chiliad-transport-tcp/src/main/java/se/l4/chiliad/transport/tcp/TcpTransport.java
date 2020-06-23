package se.l4.chiliad.transport.tcp;

import java.net.URI;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;
import se.l4.chiliad.transport.netty.AbstractNettyTransport;

public class TcpTransport
	extends AbstractNettyTransport
{
	private final int port;

	public TcpTransport(int port)
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
		return TcpServer.create()
			.doOnBind(b -> logger.info("Starting server at port " + port))
			.doOnConnection(onConnection)
			.port(port)
			.bind();
	}

	@Override
	protected Mono<? extends Connection> connect(URI uri, Consumer<? super Connection> consumer)
	{
		return TcpClient.create()
			.doOnConnected(consumer)
			.host(uri.getHost())
			.port(uri.getPort())
			.connect();
	}
}
