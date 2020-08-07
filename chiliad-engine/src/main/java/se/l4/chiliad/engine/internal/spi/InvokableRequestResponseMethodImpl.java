package se.l4.chiliad.engine.internal.spi;

import reactor.core.publisher.Mono;
import se.l4.chiliad.engine.spi.InvokableRequestResponseMethod;
import se.l4.chiliad.engine.spi.MethodInvoker;
import se.l4.chiliad.engine.spi.RequestResponseMethod;

public class InvokableRequestResponseMethodImpl
	implements InvokableRequestResponseMethod
{
	private final RequestResponseMethod contract;
	private final MethodInvoker<Mono<? extends Object>> invoker;

	public InvokableRequestResponseMethodImpl(
		RequestResponseMethod contract,
		MethodInvoker<Mono<? extends Object>> invoker
	)
	{
		this.contract = contract;
		this.invoker = invoker;
	}

	@Override
	public RequestResponseMethod getContract()
	{
		return contract;
	}

	@Override
	public Mono<? extends Object> invoke(Object... args)
	{
		return invoker.invoke(args);
	}
}
