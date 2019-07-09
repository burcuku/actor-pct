Systematic testing of Akka actor programs
=========================

This project implementes a systematic exploration of actor programs.

### Building and running an example app:

Requirements:

- Java 8 SDK
- Scala 2.12
- [Scala Build Tool](http://www.scala-sbt.org/) 



Build the project and publish its libraries locally:

```
cd actor-testing
sbt compile
sbt publishLocal
sbt publishM2  // to use in the Maven projects
```


Run the server using the following command. After this step, the Tcp server for the explorer should be bound.

```
sbt explorer/run
```

Run the application (make sure that the application's dispatcher is configured to listen from the network in its "dispatcher.conf" file). 
When run, the application dispatcher should connect to the server and process the requests.

```
cd apps/pingpong
sbt clean compile run
```
