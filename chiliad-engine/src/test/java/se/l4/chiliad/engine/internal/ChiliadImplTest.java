package se.l4.chiliad.engine.internal;

import java.net.URI;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.Chiliad;
import se.l4.chiliad.engine.TestTransport;
import se.l4.chiliad.engine.auth.AnonymousAuth;
import se.l4.chiliad.engine.spi.RequestResponseMethod;
import se.l4.chiliad.engine.spi.ServiceContract;
import se.l4.exobytes.standard.StringSerializer;

public class ChiliadImplTest
{
	@Test
	public void testServerServerConnect()
	{
		TestTransport t1 = TestTransport.server("t1");
		TestTransport t2 = TestTransport.server("t2");

		Chiliad c1 = Chiliad.create()
			.addAuthMethod(new AnonymousAuth())
			.addTransport(t1)
			.start()
			.block();

		Chiliad c2 = Chiliad.create()
			.addAuthMethod(new AnonymousAuth())
			.addTransport(t2)
			.start()
			.block();

		c1.connect(URI.create("local://t2"))
			.block();
	}

	@Test
	public void testRegisterPreBuilt()
	{
		ServiceContract contract = ServiceContract.create("chiliad:test")
			.addMethod(
				RequestResponseMethod.create("echo")
					.withResponseSerializer(StringSerializer.INSTANCE)
					.withArgumentSerializers(StringSerializer.INSTANCE)
					.build()
			)
			.build();

		Chiliad c1 = Chiliad.create()
			.addAuthMethod(new AnonymousAuth())
			.start()
			.block();

		c1.addService(contract.implement()
			.requestResponse("echo", args -> Mono.just((String) args[0]))
			.build()
		).block();
	}
}
