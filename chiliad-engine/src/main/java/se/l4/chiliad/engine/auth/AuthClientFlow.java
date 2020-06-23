package se.l4.chiliad.engine.auth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;

public interface AuthClientFlow
{
	/**
	 * Get the initial message of this flow. This will be sent to the server
	 * and received by {@link AuthServerFlow#receiveData}.
	 *
	 * @param allocator
	 * @return
	 */
	Mono<ByteBuf> initialMessage(ByteBufAllocator allocator);

	/**
	 * Receive some data as sent by the server.
	 *
	 * @param data
	 * @return
	 */
	Mono<AuthReply> receiveData(ByteBuf data);

	/**
	 * Dispose of this flow.
	 *
	 * @return
	 */
	Mono<Void> dispose();
}
