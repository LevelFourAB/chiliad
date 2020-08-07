package se.l4.chiliad.engine.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.Chiliad;
import se.l4.chiliad.engine.ServiceEvent;
import se.l4.chiliad.engine.TestTransport;
import se.l4.chiliad.engine.auth.AnonymousAuth;
import se.l4.chiliad.engine.spi.InvokableRequestResponseMethod;
import se.l4.chiliad.engine.spi.InvokableService;
import se.l4.chiliad.engine.spi.RequestResponseMethod;
import se.l4.chiliad.engine.spi.ServiceContract;
import se.l4.exobytes.standard.StringSerializer;

public class ChiliadServiceCallsTest
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
	public void testClientCanCallServer()
	{
		ServiceContract contract = ServiceContract.create("chiliad:test")
			.addMethod(
				RequestResponseMethod.create("echo")
					.withResponseSerializer(StringSerializer.INSTANCE)
					.withArgumentSerializers(StringSerializer.INSTANCE)
					.build()
			)
			.build();

		c1.addService(contract.implement()
			.requestResponse("echo", args -> Mono.just((String) args[0]))
			.build()
		).block();

		// C2 connects to C1 - C2 becomes the client
		c2.connect(URI.create("local://t1"))
			.block();

		// Wait for C2 to see the chiliad:test service
		c2.services()
			.filter(ServiceEvent.isAvailable("chiliad:test"))
			.blockFirst(Duration.ofSeconds(1));

		InvokableService service = c2.getRemoteService(contract);

		Object o = service.getMethod("echo")
			.cast(InvokableRequestResponseMethod.class)
			.flatMap(m -> m.invoke("test"))
			.block();

		assertThat(o, is("test"));
	}

	@Test
	public void testServerCanCallClient()
	{
		ServiceContract contract = ServiceContract.create("chiliad:test")
			.addMethod(
				RequestResponseMethod.create("echo")
					.withResponseSerializer(StringSerializer.INSTANCE)
					.withArgumentSerializers(StringSerializer.INSTANCE)
					.build()
			)
			.build();

		c1.addService(contract.implement()
			.requestResponse("echo", args -> Mono.just((String) args[0]))
			.build()
		).block();

		// C1 connects to C2 - C2 becomes the server
		c1.connect(URI.create("local://t2"))
			.block();

		// Wait for C2 to see the chiliad:test service
		c2.services()
			.filter(ServiceEvent.isAvailable("chiliad:test"))
			.blockFirst(Duration.ofSeconds(1));

		InvokableService service = c2.getRemoteService(contract);

		Object o = service.getMethod("echo")
			.cast(InvokableRequestResponseMethod.class)
			.flatMap(m -> m.invoke("test"))
			.block();

		assertThat(o, is("test"));
	}
}
