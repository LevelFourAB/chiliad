package se.l4.chiliad.transport.tcp;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.rsocket.Closeable;
import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.auth.AnonymousAuth;
import se.l4.chiliad.engine.transport.DefaultTransportContext;

public class TcpTransportTest
{
	@Test
	public void testClientConnect()
	{
		DefaultTransportContext context = new DefaultTransportContext(
			Collections.singletonList(new AnonymousAuth())
		);

		TcpTransport transport = new TcpTransport(45120);
		Closeable c = transport.serve(context, conn -> Mono.empty()).block();

		transport.connect(context, URI.create("chiliad+tcp://127.0.0.1:45120"))
			.block(Duration.ofSeconds(5));
	}
}
