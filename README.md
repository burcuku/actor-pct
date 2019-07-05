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


Run the server:

```
sbt explorer/run
```

  