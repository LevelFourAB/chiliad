package se.l4.chiliad.engine.internal.remote;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import se.l4.chiliad.ServiceException;
import se.l4.chiliad.engine.ServiceEvent;
import se.l4.chiliad.engine.internal.local.LocalServicesManager;
import se.l4.chiliad.engine.spi.InvokableRequestResponseMethod;
import se.l4.chiliad.engine.spi.InvokableService;
import se.l4.chiliad.engine.spi.RequestResponseMethod;
import se.l4.chiliad.engine.spi.ServiceContract;
import se.l4.exobytes.standard.StringSerializer;

public class RemoteServicesManagerTest
{
	@Test
	public void testRegisterEmitsRemoteEvent()
	{
		LocalServicesManager local = new LocalServicesManager();
		RemoteServicesManager remote = new RemoteServicesManager();

		ServiceContract contract = ServiceContract.create("chiliad:test")
			.addMethod(
				RequestResponseMethod.create("echo")
					.withResponseSerializer(StringSerializer.INSTANCE)
					.withArgumentSerializers(StringSerializer.INSTANCE)
					.build()
			)
			.build();

		remote.registerRemote(local.incomingHandler());

		StepVerifier verifier = StepVerifier.create(remote.events())
			.expectNext(new ServiceEvent(ServiceEvent.Type.AVAILABLE, "chiliad:test"))
			.thenCancel()
			.verifyLater();

		local.register(contract.implement()
			.requestResponse("echo", args -> Mono.just((String) args[0]))
			.build()
		).block();

		verifier.verify(Duration.ofSeconds(1));
	}

	@Test
	public void testGetRemote()
	{
		RemoteServicesManager remote = new RemoteServicesManager();

		ServiceContract contract = ServiceContract.create("chiliad:test")
			.addMethod(
				RequestResponseMethod.create("echo")
					.withResponseSerializer(StringSerializer.INSTANCE)
					.withArgumentSerializers(StringSerializer.INSTANCE)
					.build()
			)
			.build();

		InvokableService service = remote.createRemoteService(contract);

		assertThat(service, notNullValue());
	}

	@Test
	public void testInvokeRemoteWithoutPeers()
	{
		RemoteServicesManager remote = new RemoteServicesManager();

		ServiceContract contract = ServiceContract.create("chiliad:test")
			.addMethod(
				RequestResponseMethod.create("echo")
					.withResponseSerializer(StringSerializer.INSTANCE)
					.withArgumentSerializers(StringSerializer.INSTANCE)
					.build()
			)
			.build();

		InvokableService service = remote.createRemoteService(contract);

		assertThrows(ServiceException.class, () -> {
			 service.getMethod("echo")
				.cast(InvokableRequestResponseMethod.class)
				.flatMap(m -> m.invoke("test"))
				.block();
		});
	}

	@Test
	public void testInvokeRemote()
	{
		ServiceContract contract = ServiceContract.create("chiliad:test")
			.addMethod(
				RequestResponseMethod.create("echo")
					.withResponseSerializer(StringSerializer.INSTANCE)
					.withArgumentSerializers(StringSerializer.INSTANCE)
					.build()
			)
			.build();

		LocalServicesManager local = new LocalServicesManager();
		RemoteServicesManager remote = new RemoteServicesManager();

		remote.registerRemote(local.incomingHandler());

		local.register(contract.implement()
			.requestResponse("echo", args -> Mono.just((String) args[0]))
			.build()
		).block();

		InvokableService service = remote.createRemoteService(contract);

		Object o = service.getMethod("echo")
			.cast(InvokableRequestResponseMethod.class)
			.flatMap(m -> m.invoke("test"))
			.block();

		assertThat(o, is("test"));
	}
}
