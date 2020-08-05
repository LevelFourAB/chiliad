package se.l4.chiliad.engine.internal.local;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.rsocket.RSocket;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import se.l4.chiliad.engine.internal.protocol.RemoteServiceEvent;
import se.l4.chiliad.engine.internal.remote.RemoteInvokers;
import se.l4.chiliad.engine.spi.InvokableRequestResponseMethod;
import se.l4.chiliad.engine.spi.RequestResponseMethod;
import se.l4.chiliad.engine.spi.ServiceContract;
import se.l4.exobytes.standard.StringSerializer;

public class LocalServicesManagerTest
{
	@Test
	public void testRegister()
	{
		LocalServicesManager local = new LocalServicesManager();

		ServiceContract contract = ServiceContract.create("chiliad:test")
			.addMethod(
				RequestResponseMethod.create("echo")
					.withResponseSerializer(StringSerializer.INSTANCE)
					.withArgumentSerializers(StringSerializer.INSTANCE)
					.build()
			)
			.build();

		local.register(contract.implement()
			.requestResponse("echo", args -> Mono.just((String) args[0]))
			.build()
		).block();
	}

	@Test
	public void testEmitsEventOnExistingService()
	{
		LocalServicesManager local = new LocalServicesManager();

		ServiceContract contract = ServiceContract.create("chiliad:test")
			.addMethod(
				RequestResponseMethod.create("echo")
					.withResponseSerializer(StringSerializer.INSTANCE)
					.withArgumentSerializers(StringSerializer.INSTANCE)
					.build()
			)
			.build();

		local.register(contract.implement()
			.requestResponse("echo", args -> Mono.just((String) args[0]))
			.build()
		).block();

		StepVerifier.create(local.currentAndFutureServiceEvents())
			.expectNext(RemoteServiceEvent.available("chiliad:test"))
			.thenCancel()
			.verify(Duration.ofSeconds(1));
	}

	@Test
	public void testEmitsEventOnNewService()
	{
		LocalServicesManager local = new LocalServicesManager();

		StepVerifier verifier = StepVerifier.create(local.currentAndFutureServiceEvents())
			.expectNext(RemoteServiceEvent.available("chiliad:test"))
			.thenCancel()
			.verifyLater();

		ServiceContract contract = ServiceContract.create("chiliad:test")
			.addMethod(
				RequestResponseMethod.create("echo")
					.withResponseSerializer(StringSerializer.INSTANCE)
					.withArgumentSerializers(StringSerializer.INSTANCE)
					.build()
			)
			.build();

		local.register(contract.implement()
			.requestResponse("echo", args -> Mono.just((String) args[0]))
			.build()
		).block();

		verifier.verify(Duration.ofSeconds(1));
	}

	@Test
	public void testEmitsEventOnServiceRemoval()
	{
		LocalServicesManager local = new LocalServicesManager();

		ServiceContract contract = ServiceContract.create("chiliad:test")
			.addMethod(
				RequestResponseMethod.create("echo")
					.withResponseSerializer(StringSerializer.INSTANCE)
					.withArgumentSerializers(StringSerializer.INSTANCE)
					.build()
			)
			.build();

		Disposable d = local.register(contract.implement()
			.requestResponse("echo", args -> Mono.just((String) args[0]))
			.build()
		).block();

		StepVerifier verifier = StepVerifier.create(local.currentAndFutureServiceEvents())
			.expectNext(RemoteServiceEvent.available("chiliad:test"))
			.expectNext(RemoteServiceEvent.unavailable("chiliad:test"))
			.thenCancel()
			.verifyLater();

		d.dispose();

		verifier.verify(Duration.ofSeconds(1));
	}

	@Test
	public void testRegisterAndCall()
	{
		LocalServicesManager local = new LocalServicesManager();

		ServiceContract contract = ServiceContract.create("chiliad:test")
			.addMethod(
				RequestResponseMethod.create("echo")
					.withResponseSerializer(StringSerializer.INSTANCE)
					.withArgumentSerializers(StringSerializer.INSTANCE)
					.build()
			)
			.build();

		local.register(contract.implement()
			.requestResponse("echo", args -> Mono.just((String) args[0]))
			.build()
		).block();

		RSocket socket = local.incomingHandler();

		RequestResponseMethod echoMethod = (RequestResponseMethod) contract.getMethod("echo")
			.block();
		InvokableRequestResponseMethod invokable = echoMethod.toInvokable(
			RemoteInvokers.requestResponse("chiliad:test", echoMethod, () -> socket)
		);

		Object result = invokable.invoke("hello")
			.block(Duration.ofSeconds(2));

		assertEquals(result, "hello");
	}
}
