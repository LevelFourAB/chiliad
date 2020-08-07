package se.l4.chiliad.engine.internal.reflection;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import se.l4.chiliad.RemoteMethod;
import se.l4.chiliad.RemoteName;
import se.l4.chiliad.Service;
import se.l4.chiliad.engine.spi.RequestResponseMethod;
import se.l4.chiliad.engine.spi.ServiceContract;
import se.l4.chiliad.engine.spi.ServiceMethod;
import se.l4.exobytes.Serializers;
import se.l4.ylem.types.reflect.Types;

public class ServiceContractGeneratorTest
{
	private ServiceContractGenerator generator;

	@BeforeEach
	public void prepare()
	{
		generator = new ServiceContractGenerator(Serializers.create()
			.build());
	}

	@Test
	public void testEmptyService()
	{
		ServiceContract contract = generator.generate(Types.reference(EmptyService.class));

		assertThat(contract.getName(), is("a"));
		assertThat(contract.getMethods().size(), is(0));
	}

	@RemoteName("a")
	interface EmptyService
		extends Service
	{
	}

	@Test
	public void testSingleRequestResponse()
	{
		ServiceContract contract = generator.generate(Types.reference(SingleRequestResponse.class));

		assertThat(contract.getName(), is("b"));
		assertThat(contract.getMethods().size(), is(1));

		ServiceMethod echoMethod = contract.getMethod("echo")
			.block();
		assertThat(echoMethod, instanceOf(RequestResponseMethod.class));
	}

	@RemoteName("b")
	interface SingleRequestResponse
		extends Service
	{
		@RemoteMethod
		Mono<String> echo(String result);
	}

	@Test
	public void testConcreteRequestResponse()
	{
		ServiceContract contract = generator.generate(Types.reference(ConcreteRequestResponse.class));

		assertThat(contract.getName(), is("b"));
		assertThat(contract.getMethods().size(), is(1));

		ServiceMethod echoMethod = contract.getMethod("echo")
			.block();
		assertThat(echoMethod, instanceOf(RequestResponseMethod.class));
	}

	class ConcreteRequestResponse
		implements SingleRequestResponse
	{
		@Override
		public Mono<String> echo(String result)
		{
			return null;
		}
	}
}
