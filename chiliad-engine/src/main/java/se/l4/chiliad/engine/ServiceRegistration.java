package se.l4.chiliad.engine;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public interface ServiceRegistration
{
	Mono<Disposable> register();
}
