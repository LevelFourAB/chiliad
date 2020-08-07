# Chiliad

Chiliad is a Java 9+ RPC library for [reactive streams](https://www.reactive-streams.org/)
built on top of [RSocket](https://rsocket.io/) and [Project Reactor](https://projectreactor.io/). 
Chiliad is built around annotating and implementing interfaces as way to build
and consume remote services.

```java
// Start an instace
Chiliad instance = Chiliad.create()
  .addTransport(TCPTransport.create().withPort(7010).build())
  .start()
  .block();

// Request that we connect to something
instance.connect(URI.create("chiliad+tcp://127.0.0.1:7011"))
  .subscribe();

// Create a remote service - using the default settings
EchoService service = instance.createRemoteService(EchoService.class)
  .build()
  .block();
```

## Service contracts

Java interfaces act as contracts for services, and can either be implemented
or fetched as remote services.

Example of a contract:

```java
@RemoteName("test:echo")
interface EchoService extends Service {
  /**
   * Reactive style echo - mono not executed until subscribed.
   */
  @RemoteMethod
  Mono<String> echoReactive(String value);

  /**
   * Reactive style echo multiple - flux will emit N times when subscribed.
   */
  @RemoteMethod
  Flux<String> echoAllReactive(String value, int times);
}
```

## Implementing a service

To implement a service create a class that implements the interface that is 
the service contract:

```java
class EchoServiceImpl implements EchoService {
  @Override
  public Mono<String> echoReactive(String value) {
    return Mono.just(value);
  }

  @Override
  public Flux<String> echoAllReactive(String value, int times) {
    return Mono.just(value).repeat(times);
  }
}
```

This can then be registered with the `Chiliad` instance:

```java
instance.addService(new EchoServiceImpl())
  .register()
  .block();
```

## Using a remote service

Using a remote service is done using by fetching the service using the service
contract interface:

```java
// Get a registered service
EchoService service = instance.createRemoteService(EchoService.class)
  .build()
  .block();
```

Methods on the returned service can then be invoked as normal:

```java
// Echo test 10 times
service.echoAllReactive("test", 10)
  .subscribe(value -> System.out.println("Got " + value));
```
