### broad concepts

Fintrospect is a library designed to facilitate painless definition, serving and consumption of HTTP APIs. It uses the following main concepts:

- ```RouteSpec```: defines the overall HTTP contract of an endpoint. This contract can then be bound to a Finagle Service representing an HTTP client, or bundled into a Module and attached to a Finagle HTTP server.
- ```ParameterSpec```: defines the acceptable format for a request parameter (Path/Query/Header/Form-field). Provides the auto-marshalling mechanic for serializing and deserializing objects to and from HTTP message.
- ```BodySpec```: similar to ParameterSpec, but applied to the body of an HTTP message.
- ```ModuleSpec```: defines a set of Routes which are grouped under a particular request path. These modules can be combined and then converted to a Finagle service and attached to a Finagle HTTP server. Each module provides an 
endpoint under which it's own runtime-generated documentation can be served (eg. in Swagger format).

Note that in order to aid the reader, the code in this guide has omitted imports that would have made the it read more nicely. The sacrifices we make in the name of learning... :)

### regarding finagle
Since Fintrospect is build on top of Finagle, it's worth acquainting yourself with it's concepts, which can be found <a href="http://twitter.github.io/finagle/guide" target="_top">here</a>. 

#### &tldr; finagle primer:
1. Finagle provides protocol-agnostic RPC and is based on Netty
2. It is mainly asynchronous and makes heavy usage of Twitter's version of Scala Futures
3. It defines uniform ```Service``` and ```Filter``` interfaces for both client and server APIs that are effectively a single method...
```scala
Service:  def apply(request : Request) : Future[Response]
Filter:   def apply(request : RequestIn, service : Service[RequestOut, ResponseIn]) : Future[ResponseOut]
```
4. ```Filters``` can be chained together and then applied to a ```Service```, which results in another ```Service```. This is useful to 
apply layers of functionality such as caching headers, retry behaviour, and timeouts.