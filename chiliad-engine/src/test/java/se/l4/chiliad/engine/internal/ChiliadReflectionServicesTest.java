package se.l4.chiliad.engine.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import se.l4.chiliad.RemoteMethod;
import se.l4.chiliad.RemoteName;
import se.l4.chiliad.Service;
import se.l4.chiliad.engine.Chiliad;
import se.l4.chiliad.engine.ServiceEvent;
import se.l4.chiliad.engine.TestTransport;
import se.l4.chiliad.engine.auth.AnonymousAuth;

public class ChiliadReflectionServicesTest
{
	private static final TestTransport t1 = TestTransport.server("t1");
	private static final TestTransport t2 = TestTransport.server("t2");

	private Chiliad c1;
	private Chiliad c2;

	@BeforeEach
	public void setup()
	{
		c1 = Chiliad.create()
			.addAuthMethod(new AnonymousAuth())
			.addTransport(t1)
			.start()
			.block();

		c2 = Chiliad.create()
			.addAuthMethod(new AnonymousAuth())
			.addTransport(t2)
			.start()
			.block();
	}

	@AfterEach
	public void dispose()
	{
		c1.dispose();
		c2.dispose();
	}

	@Test
	public void testRemoteCanSeeReflectionService()
	{
		c1.addService(new EchoServiceImpl())
			.register()
			.block();

		// C2 connects to C1 - C2 becomes the client
		c2.connect(URI.create("local://t1"))
			.block();

		// Wait for C2 to see the chiliad:test service
		c2.services()
			.filter(ServiceEvent.isAvailable("chiliad:echo"))
			.blockFirst(Duration.ofSeconds(1));
	}

	@Test
	public void testRemoteCanInvokeReflectionService()
	{
		c1.addService(new EchoServiceImpl())
			.register()
			.block();

		// C2 connects to C1 - C2 becomes the client
		c2.connect(URI.create("local://t1"))
			.block();

		// Wait for C2 to see the chiliad:echo service
		c2.services()
			.filter(ServiceEvent.isAvailable("chiliad:echo"))
			.blockFirst(Duration.ofSeconds(1));

		EchoService service = c2.createRemoteService(EchoService.class)
			.build()
			.block();

		String result = service.echo("test")
			.block(Duration.ofSeconds(1));

		assertThat(result, is("test"));
	}

	@RemoteName("chiliad:echo")
	public interface EchoService
		extends Service
	{
		@RemoteMethod
		Mono<String> echo(String value);
	}

	public class EchoServiceImpl
		implements EchoService
	{
		@Override
		public Mono<String> echo(String value)
		{
			return Mono.just(value);
		}
	}
}
