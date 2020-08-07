package se.l4.chiliad.transport.tcp;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.rsocket.Closeable;
import io.rsocket.DuplexConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.concurrent.Queues;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import se.l4.chiliad.engine.auth.AnonymousAuth;
import se.l4.chiliad.engine.transport.DefaultTransportContext;

public class TCPTransportTest
{
	private Closeable server;

	@AfterEach
	public void close()
	{
		if(server != null)
		{
			server.dispose();
		}
	}

	@Test
	public void testClientConnect()
	{
		DefaultTransportContext context = new DefaultTransportContext(
			Lists.immutable.of(new AnonymousAuth())
		);

		TCPTransport transport = new TCPTransport(45120);
		server = transport.serve(context, conn -> Mono.empty()).block(Duration.ofSeconds(5));

		transport.connect(context, URI.create("chiliad+tcp://127.0.0.1:45120"))
			.block(Duration.ofSeconds(5));
	}

	private Tuple2<DuplexConnection, DuplexConnection> createConnection()
	{
		DefaultTransportContext context = new DefaultTransportContext(
			Lists.immutable.of(new AnonymousAuth())
		);

		int port = ThreadLocalRandom.current().nextInt(11000, 14000);
		TCPTransport transport = new TCPTransport(port);

		MonoProcessor<DuplexConnection> connection = MonoProcessor.create();
		server = transport.serve(context, conn -> {
			connection.onNext(conn);
			return Mono.empty();
		}).block(Duration.ofSeconds(5));

		DuplexConnection client = transport.connect(context, URI.create("chiliad+tcp://127.0.0.1:" + port))
			.block(Duration.ofSeconds(5));

		client.onClose().subscribe(v -> System.out.println("Client closed"));

		DuplexConnection server = connection.block(Duration.ofSeconds(5));

		server.onClose().subscribe(v -> System.out.println("Client closed"));

		return Tuples.of(server, client);
	}

	@Test
	public void testServerEcho()
	{
		Tuple2<DuplexConnection, DuplexConnection> connections = createConnection();

		UnicastProcessor<ByteBuf> echoer = UnicastProcessor.create(Queues.<ByteBuf>small().get());
		connections.getT1().send(echoer).subscribe();
		connections.getT1().receive()
			.map(frame -> frame.retain())
			.limitRate(10)
			.subscribe(echoer);

		Flux<ByteBuf> outgoing = Flux.range(0, 10_000)
			.map(i -> {
				ByteBuf buffer = connections.getT2().alloc().buffer();
				buffer.writeInt(i);
				for(int j=0; j<4; j++)
				{
					buffer.writeInt(j);
				}
				return buffer;
			});

		StepVerifier verifier = connections.getT2().receive()
			.as(StepVerifier::create)
			.expectNextSequence(outgoing.collectList().block())
			.thenCancel();

		connections.getT2().send(outgoing).subscribe();

		verifier.verify(Duration.ofSeconds(120));
	}

	@Test
	public void testServerParallelEcho()
		throws Exception
	{
		Tuple2<DuplexConnection, DuplexConnection> connections = createConnection();

		UnicastProcessor<ByteBuf> echoer = UnicastProcessor.create(Queues.<ByteBuf>small().get());
		FluxSink<ByteBuf> echoerSink = echoer.sink();
		connections.getT1().send(echoer).subscribe();
		connections.getT1().receive()
			.subscribe(frame -> echoerSink.next(frame.retain()));

		Flux<ByteBuf> outgoing = Flux.range(0, 1024)
			.map(i -> {
				ByteBuf buffer = connections.getT2().alloc().buffer();
				buffer.writeInt(i);
				for(int j=0; j<100; j++)
				{
					buffer.writeInt(j);
				}
				return buffer;
			});

		int sequences = 64;
		StepVerifier verifier = connections.getT2().receive()
			.as(StepVerifier::create)
			.expectNextCount(sequences * 1024)
			.thenCancel();

		Flux.range(0, sequences)
			.flatMap(r -> connections.getT2().send(outgoing).subscribeOn(Schedulers.elastic()), 12)
			.subscribe();

		verifier.verify(Duration.ofSeconds(5));
	}
}
