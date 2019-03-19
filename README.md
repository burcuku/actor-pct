akka-pct-dispatcher
=========================

A custom Akka dispatcher which:

- Logs the actor creation, message send, message receive and actor termination events.
- Enforces a particular delivery order of the messages to the actors. 

The dispatcher intercepts and keeps all actor messages. Before delivering an actor message, it displays the intercepted messages and asks for the user input to select the next delivery.


### Building and running an example app with debugging-dispatcher:

Requirements:

- Java 8 SDK
- Scala 2.12
- [Scala Build Tool](http://www.scala-sbt.org/) 

Build the project and publish its libraries locally:

```
cd dispatcher
sbt compile
sbt publishLocal
sbt publishM2  // to use in the Maven projects
```


Run the example application:

```
cd apps/pingpong
sbt run
```

To add the dispatcher to your maven project dependencies, add the following to your pom.xml.

 ```
 <dependency>
   <groupId>com.pct</groupId>
   <artifactId>actor-pct_2.11</artifactId>
   <version>1.0</version>
 </dependency>
  ```
  
  