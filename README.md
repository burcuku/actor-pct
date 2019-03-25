Randomized testing of Akka actor programs
=========================

This project implementes **RandomWalk**, **POS**, **d-POS**, **PCTCP** and **taPCTCP** schedulers for Akka actor programs.

### Building and running an example app:

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

The test parameters can be configured using [```dispatcher.conf```](https://gitlab.mpi-sws.org/burcu/actor-pct/blob/master/apps/pingpong/src/main/resources/dispatcher.conf) file. 

To add the dispatcher to your maven project dependencies, add the following to your pom.xml.

 ```
 <dependency>
   <groupId>org.mpisws.actortest</groupId>
   <artifactId>actor-scheduler_2.12</artifactId>
   <version>1.0</version>
 </dependency>
  ```
 
  