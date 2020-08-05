package se.l4.chiliad.engine.internal.spi;

import java.util.function.Function;

import reactor.core.publisher.Flux;
import se.l4.chiliad.engine.spi.InvokableRequestStreamMethod;
import se.l4.chiliad.engine.spi.RequestStreamMethod;

public class InvokableRequestStreamMethodImpl
	implements InvokableRequestStreamMethod
{
	private final RequestStreamMethod contract;
	private final Function<Object[], Flux<? extends Object>> invoker;

	public InvokableRequestStreamMethodImpl(
		RequestStreamMethod contract,
		Function<Object[], Flux<? extends Object>> invoker
	)
	{
		this.contract = contract;
		this.invoker = invoker;
	}

	@Override
	public RequestStreamMethod getContract()
	{
		return contract;
	}

	@Override
	public Flux<? extends Object> invoke(Object[] args)
	{
		return invoker.apply(args);
	}
}
