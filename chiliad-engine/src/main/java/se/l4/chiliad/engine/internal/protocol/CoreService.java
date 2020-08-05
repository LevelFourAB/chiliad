package se.l4.chiliad.engine.internal.protocol;

import se.l4.chiliad.engine.spi.RequestStreamMethod;
import se.l4.chiliad.engine.spi.ServiceContract;

/**
 * Things related to the core service used by Chiliad to tell peers about
 * the services available.
 */
public class CoreService
{
	public static final String NAME = "chiliad:core";

	public static final RequestStreamMethod SERVICES_METHOD = RequestStreamMethod.create("services")
		.withResponseSerializer(new RemoteServiceEvent.SerializerImpl())
		.withArgumentSerializers()
		.build();

	public static final ServiceContract SERVICE = ServiceContract
		.create(NAME)
		.addMethod(SERVICES_METHOD)
		.build();

	private CoreService()
	{
	}
}
