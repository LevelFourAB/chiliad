package se.l4.chiliad.engine.auth;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;

public interface AuthServerFlow
{
	/**
	 * Receive data from the client.
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
