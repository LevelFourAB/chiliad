package se.l4.chiliad.engine.auth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;

/**
 * Anonymous authentication.
 */
public class AnonymousAuth
	implements AuthMethod
{
	@Override
	public String getId()
	{
		return "anonymous";
	}

	@Override
	public Mono<AuthClientFlow> createClient()
	{
		return Mono.just(new AuthClientFlow()
		{
			@Override
			public Mono<ByteBuf> initialMessage(ByteBufAllocator allocator)
			{
				return Mono.just(allocator.buffer());
			}

			@Override
			public Mono<AuthReply> receiveData(ByteBuf data)
			{
				return Mono.error(new AuthException("Additional data not supported"));
			}

			@Override
			public Mono<Void> dispose()
			{
				return Mono.empty();
			}
		});
	}

	@Override
	public Mono<AuthServerFlow> createServer()
	{
		return Mono.just(new AuthServerFlow()
		{
			@Override
			public Mono<AuthReply> receiveData(ByteBuf data)
			{
				return Mono.just(
					data.readableBytes() == 0 ? new AuthOk() : new AuthReject()
				);
			}

			@Override
			public Mono<Void> dispose()
			{
				return Mono.empty();
			}
		});
	}
}
