package akka.dispatch

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.dispatch.state.Messages.Message
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.dispatch.state.{DependencyGraphBuilder => DGB}
import akka.dispatch.util.ReflectionUtils

class DependencyGraphBuilderTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  private val main = TestActorRef[Dummy]
  private val executor = TestActorRef[Dummy]
  private val terminator = TestActorRef[Dummy]
  private val writer = TestActorRef[Dummy]
  private val env = TestActorRef[Dummy]
  private val visitor = TestActorRef[Dummy]

  private val executeEnvelope = ReflectionUtils.createNewEnvelope("Execute", env)
  private val writeEnvelope = ReflectionUtils.createNewEnvelope("Write", main)
  private val actionDoneEnvelope = ReflectionUtils.createNewEnvelope("ActionDone", main)
  private val flushEnvelope = ReflectionUtils.createNewEnvelope("Flush", terminator)
  private val flushedEnvelope = ReflectionUtils.createNewEnvelope("Flushed", writer)
  private val visitorEnvelope = ReflectionUtils.createNewEnvelope("VisitorMsg", writer)
  private val mainEnvelope = ReflectionUtils.createNewEnvelope("MainMsg", visitor)
  private val toTerminatorEnvelope = ReflectionUtils.createNewEnvelope("TerminatorGetsBeforeTerminateMsg", main)


  // input to dependency graph builder:
  // (received: Message, sent: List[Message], created: List[ActorRef])

  /*
    Initial sequence of program events:
    MessageReceived(ActorRef.noSender, Envelope("", ActorRef.noSender)),//0
    ActorCreated(main), ActorCreated(executor), ActorCreated(terminator), ActorCreated(writer),
    MessageSent(main, executeEnvelope), //1
    MessageSent(terminator, toTerminatorEnvelope) //2
  */
  val programOutput1: (Message, List[Message], List[ActorRef]) = (
    Message(0L, ActorRef.noSender, ReflectionUtils.createNewEnvelope("", ActorRef.noSender)),
    List(Message(1L, main, executeEnvelope), Message(2L, terminator, toTerminatorEnvelope)),
    List(main, executor, terminator, writer)
  )

  /*
    Next sequence of program events:
    MessageReceived(main, executeEnvelope), //1
    MessageSent(writer, writeEnvelope), //3
    MessageSent(terminator, actionDoneEnvelope) //4
  */
  val programOutput2: (Message, List[Message], List[ActorRef]) = (
    Message(1L, main, executeEnvelope),
    List(Message(3L, writer, writeEnvelope), Message(4L, terminator, actionDoneEnvelope)),
    List()
  )

  /*
    Next sequence of program events:
    MessageReceived(terminator, toTerminatorEnvelope) //2
  */
  val programOutput3: (Message, List[Message], List[ActorRef]) = (
    Message(2L, terminator, toTerminatorEnvelope),
    List(),
    List()
  )

  /*
    Next sequence of program events:
    MessageReceived(terminator, actionDoneEnvelope), //4
    MessageSent(writer, flushEnvelope), //5
    ActorCreated(visitor)
  */
  val programOutput4: (Message, List[Message], List[ActorRef]) = (
    Message(4L, terminator, actionDoneEnvelope),
    List(Message(5L, writer, flushEnvelope)),
    List(visitor)
  )

  /*
    Next sequence of program events:
    MessageReceived(writer, flushEnvelope), //5
    MessageSent(terminator, flushedEnvelope), //6
    MessageSent(visitor, visitorEnvelope) //7
  */
  val programOutput5: (Message, List[Message], List[ActorRef]) = (
    Message(5L, writer, flushEnvelope),
    List(Message(6L, terminator, flushedEnvelope), Message(7L, visitor, visitorEnvelope)),
    List()
  )

  /*
    Next sequence of program events:
    MessageReceived(terminator, flushedEnvelope) //6
  */
  val programOutput6: (Message, List[Message], List[ActorRef]) = (
    Message(6L, terminator, flushedEnvelope),
    List(),
    List()
  )

  /*
    Next sequence of program events:
    MessageReceived(writer, writeEnvelope) //3
  */
  val programOutput7: (Message, List[Message], List[ActorRef]) = (
    Message(3L, writer, writeEnvelope),
    List(),
    List()
  )

  /*
    Next sequence of program events:
    MessageReceived(visitor, visitorEnvelope), //7
    MessageSent(main, mainEnvelope) //8
  */
  val programOutput8: (Message, List[Message], List[ActorRef]) = (
    Message(7L, visitor, visitorEnvelope),
    List(Message(8L, main, mainEnvelope)),
    List()
  )

  /*
    Next sequence of program events:
    MessageReceived(main, mainEnvelope) //8
  */
  val programOutput9: (Message, List[Message], List[ActorRef]) = (
    Message(8L, main, mainEnvelope),
    List(),
    List()
  )

  "A dependency builder actor" must {

    "calculate actorProcessed correctly" in {
      val dependencyBuilder = new DGB()

      dependencyBuilder.calculateDependencies(programOutput1._1, programOutput1._2, programOutput1._3)
      dependencyBuilder.calculateDependencies(programOutput2._1, programOutput2._2, programOutput2._3)

      dependencyBuilder.actorProcessed(main) shouldBe Set(Message(1, main, executeEnvelope))

      dependencyBuilder.calculateDependencies(programOutput3._1, programOutput3._2, programOutput3._3)
      dependencyBuilder.actorProcessed(main) shouldBe Set(Message(1L, main, executeEnvelope))
      dependencyBuilder.actorProcessed(terminator) shouldBe Set(Message(2L, terminator, toTerminatorEnvelope))

      dependencyBuilder.calculateDependencies(programOutput4._1, programOutput4._2, programOutput4._3)
      dependencyBuilder.actorProcessed(main) shouldBe Set(Message(1L, main, executeEnvelope))
      dependencyBuilder.actorProcessed(terminator) shouldBe Set(Message(2L, terminator, toTerminatorEnvelope), Message(4L, terminator, actionDoneEnvelope))

      dependencyBuilder.calculateDependencies(programOutput5._1, programOutput5._2, programOutput5._3)
      dependencyBuilder.actorProcessed(main) shouldBe Set(Message(1L, main, executeEnvelope))
      dependencyBuilder.actorProcessed(terminator) shouldBe Set(Message(2L, terminator, toTerminatorEnvelope), Message(4L, terminator, actionDoneEnvelope))
      dependencyBuilder.actorProcessed(writer) shouldBe Set(Message(5L, writer, flushEnvelope))

      dependencyBuilder.calculateDependencies(programOutput6._1, programOutput6._2, programOutput6._3)
      dependencyBuilder.actorProcessed(main) shouldBe Set(Message(1L, main, executeEnvelope))
      dependencyBuilder.actorProcessed(terminator) shouldBe Set(Message(2L, terminator, toTerminatorEnvelope), Message(4L, terminator, actionDoneEnvelope),
        Message(6L, terminator, flushedEnvelope))
      dependencyBuilder.actorProcessed(writer) shouldBe Set(Message(5L, writer, flushEnvelope))

      dependencyBuilder.calculateDependencies(programOutput7._1, programOutput7._2, programOutput7._3)
      dependencyBuilder.actorProcessed(main) shouldBe Set(Message(1L, main, executeEnvelope))
      dependencyBuilder.actorProcessed(terminator) shouldBe Set(Message(2L, terminator, toTerminatorEnvelope), Message(4L, terminator, actionDoneEnvelope),
        Message(6L, terminator, flushedEnvelope))
      dependencyBuilder.actorProcessed(writer) shouldBe Set(Message(5L, writer, flushEnvelope), Message(3L, writer, writeEnvelope))

      dependencyBuilder.calculateDependencies(programOutput8._1, programOutput8._2, programOutput8._3)
      dependencyBuilder.actorProcessed(main) shouldBe Set(Message(1L, main, executeEnvelope))
      dependencyBuilder.actorProcessed(terminator) shouldBe Set(Message(2L, terminator, toTerminatorEnvelope), Message(4L, terminator, actionDoneEnvelope),
        Message(6L, terminator, flushedEnvelope))
      dependencyBuilder.actorProcessed(writer) shouldBe Set(Message(5L, writer, flushEnvelope), Message(3L, writer, writeEnvelope))
      dependencyBuilder.actorProcessed(visitor) shouldBe Set(Message(7L, visitor, visitorEnvelope))

      dependencyBuilder.calculateDependencies(programOutput9._1, programOutput9._2, programOutput9._3)
      dependencyBuilder.actorProcessed(main) shouldBe Set(Message(1L, main, executeEnvelope), Message(8L, main, mainEnvelope))
      dependencyBuilder.actorProcessed(terminator) shouldBe Set(Message(2L, terminator, toTerminatorEnvelope), Message(4L, terminator, actionDoneEnvelope),
        Message(6L, terminator, flushedEnvelope))
      dependencyBuilder.actorProcessed(writer) shouldBe Set(Message(5L, writer, flushEnvelope), Message(3L, writer, writeEnvelope))
      dependencyBuilder.actorProcessed(visitor) shouldBe Set(Message(7L, visitor, visitorEnvelope))
    }

    "calculate actorCreatedBy correctly" in {
      val dependencyBuilder = new DGB()

      // InitialReceived has MessageReceived(ActorRef.noSender, Envelope("", ActorRef.noSender))
      // Actors created initially in the program (not in an actor) has ActorRef.noSender=null parent
      dependencyBuilder.calculateDependencies(programOutput1._1, programOutput1._2, programOutput1._3)
      dependencyBuilder.actorCreatedBy(main) shouldBe null
      dependencyBuilder.actorCreatedBy(executor) shouldBe null
      dependencyBuilder.actorCreatedBy(terminator) shouldBe null
      dependencyBuilder.actorCreatedBy(writer) shouldBe null

      dependencyBuilder.calculateDependencies(programOutput2._1, programOutput2._2, programOutput2._3)
      dependencyBuilder.calculateDependencies(programOutput3._1, programOutput3._2, programOutput3._3)
      dependencyBuilder.calculateDependencies(programOutput4._1, programOutput4._2, programOutput4._3)
      dependencyBuilder.actorCreatedBy(visitor) shouldBe terminator
    }

    "calculate actorCreatedIn correctly" in {
      val dependencyBuilder = new DGB()

      // InitialReceived has MessageReceived(ActorRef.noSender, Envelope("", ActorRef.noSender))
      // Actors created initially in the program (not in an actor) has ActorRef.noSender=null parent
      dependencyBuilder.calculateDependencies(programOutput1._1, programOutput1._2, programOutput1._3)
      dependencyBuilder.actorCreatedIn(main) shouldBe Message(0,null,Envelope("",null))
      dependencyBuilder.actorCreatedIn(executor) shouldBe Message(0,null,Envelope("",null))
      dependencyBuilder.actorCreatedIn(terminator) shouldBe Message(0,null,Envelope("",null))
      dependencyBuilder.actorCreatedIn(writer) shouldBe Message(0,null,Envelope("",null))

      dependencyBuilder.calculateDependencies(programOutput2._1, programOutput2._2, programOutput2._3)
      dependencyBuilder.calculateDependencies(programOutput3._1, programOutput3._2, programOutput3._3)
      dependencyBuilder.calculateDependencies(programOutput4._1, programOutput4._2, programOutput4._3)
      dependencyBuilder.actorCreatedIn(visitor) shouldBe Message(4, terminator, actionDoneEnvelope)
    }


    "calculate message send happens-before constraint  correctly" in {
      val dependencyBuilder = new DGB()

      dependencyBuilder.calculateDependencies(programOutput1._1, programOutput1._2, programOutput1._3)
      // MessageSent(main, executeEnvelope), //1
      // MessageSent(terminator, hiEnvelope) //2
      dependencyBuilder.messageSendCausality(1) shouldBe Set((DGB.MESSAGE_SEND, 0))
      dependencyBuilder.messageSendCausality(2) shouldBe Set((DGB.MESSAGE_SEND, 0))

      dependencyBuilder.calculateDependencies(programOutput2._1, programOutput2._2, programOutput2._3)
      // MessageSent(writer, writeEnvelope), //3
      // MessageSent(terminator, executionDoneEnvelope) //4
      dependencyBuilder.messageSendCausality(3) shouldBe Set((DGB.MESSAGE_SEND, 1))

      dependencyBuilder.calculateDependencies(programOutput3._1, programOutput3._2, programOutput3._3)

      dependencyBuilder.calculateDependencies(programOutput4._1, programOutput4._2, programOutput4._3)
      // MessageSent(writer, flushEnvelope), //5
      dependencyBuilder.messageSendCausality(5) shouldBe Set((DGB.MESSAGE_SEND, 4))

      dependencyBuilder.calculateDependencies(programOutput5._1, programOutput5._2, programOutput5._3)
      // MessageSent(terminator, flushedEnvelope), //6
      // MessageSent(visitor, visitorEnvelope) //7
      dependencyBuilder.messageSendCausality(6) shouldBe Set((DGB.MESSAGE_SEND, 5))
      dependencyBuilder.messageSendCausality(7) shouldBe Set((DGB.MESSAGE_SEND, 5))

      dependencyBuilder.calculateDependencies(programOutput6._1, programOutput6._2, programOutput6._3)
      dependencyBuilder.calculateDependencies(programOutput7._1, programOutput7._2, programOutput7._3)

      dependencyBuilder.calculateDependencies(programOutput8._1, programOutput8._2, programOutput8._3)
      //MessageSent(main, mainEnvelope) //8
      dependencyBuilder.messageSendCausality(8) shouldBe Set((DGB.MESSAGE_SEND, 7))
    }

    /**
      * If the receiver of a message mj is created in a message mi, mi -> mj
      */
    "calculate creator happens-before constraint correctly" in {
      val dependencyBuilder = new DGB()

      dependencyBuilder.calculateDependencies(programOutput1._1, programOutput1._2, programOutput1._3)
      // ActorCreated(main)
      // ActorCreated(executor)
      // ActorCreated(terminator)
      // ActorCreated(writer),
      // MessageSent(main, executeEnvelope), //1
      // MessageSent(terminator, toTerminatorEnvelope) //2
      dependencyBuilder.creatorCausality(1) shouldBe Set((DGB.CREATOR, 0))
      dependencyBuilder.creatorCausality(1) shouldBe Set((DGB.CREATOR, 0))

      dependencyBuilder.calculateDependencies(programOutput2._1, programOutput2._2, programOutput2._3)
      //MessageSent(writer, writeEnvelope), //3
      //MessageSent(terminator, executionDoneEnvelope) //4
      dependencyBuilder.creatorCausality(3) shouldBe Set((DGB.CREATOR, 0))
      dependencyBuilder.creatorCausality(4) shouldBe Set((DGB.CREATOR, 0))


      dependencyBuilder.calculateDependencies(programOutput3._1, programOutput3._2, programOutput3._3)
      dependencyBuilder.calculateDependencies(programOutput4._1, programOutput4._2, programOutput4._3)
      // ActorCreated(visitor)
      // MessageSent(writer, flushEnvelope), //5
      dependencyBuilder.creatorCausality(5) shouldBe Set((DGB.CREATOR, 0))

      dependencyBuilder.calculateDependencies(programOutput5._1, programOutput5._2, programOutput5._3)
      //MessageSent(terminator, flushedEnvelope) //6
      //MessageSent(visitor, visitorEnvelope) //7
      dependencyBuilder.creatorCausality(6) shouldBe Set((DGB.CREATOR, 0))
      dependencyBuilder.creatorCausality(7) shouldBe Set((DGB.CREATOR, 4))

      dependencyBuilder.calculateDependencies(programOutput8._1, programOutput8._2, programOutput8._3)
      //MessageSent(main, mainEnvelope) //8
      dependencyBuilder.creatorCausality(8) shouldBe Set((DGB.CREATOR, 0))
    }

    /*
     * If a message mj is sent in a message mk on an actor A, for all the messages i,k,j processed by A before k: mi -> mj
     */
    "calculate executed-on-sender happens-before constraint correctly" in {
      val dependencyBuilder = new DGB()

      dependencyBuilder.calculateDependencies(programOutput1._1, programOutput1._2, programOutput1._3)
      // MessageSent(main, executeEnvelope), //1
      // MessageSent(terminator, toTerminatorEnvelope) //2
      dependencyBuilder.executedOnSenderCausality(1) shouldBe Set() //nothing is processed yet
      dependencyBuilder.executedOnSenderCausality(2) shouldBe Set()

      dependencyBuilder.calculateDependencies(programOutput2._1, programOutput2._2, programOutput2._3)
      // MessageSent(writer, writeEnvelope), //3
      // MessageSent(terminator, executionDoneEnvelope) //4
      dependencyBuilder.executedOnSenderCausality(3) shouldBe Set()
      dependencyBuilder.executedOnSenderCausality(4) shouldBe Set()

      dependencyBuilder.calculateDependencies(programOutput3._1, programOutput3._2, programOutput3._3)

      dependencyBuilder.calculateDependencies(programOutput4._1, programOutput4._2, programOutput4._3)
      // MessageSent(writer, flushEnvelope), //5
      dependencyBuilder.executedOnSenderCausality(5) shouldBe Set((DGB.EXECUTED_ON_SENDER, 2)) // terminator processed 2 before this

      dependencyBuilder.calculateDependencies(programOutput5._1, programOutput5._2, programOutput5._3)
      // MessageSent(terminator, flushedEnvelope), //6
      // MessageSent(visitor, visitorEnvelope) //7
      dependencyBuilder.executedOnSenderCausality(6) shouldBe Set() // no messages are processed in writer before
      dependencyBuilder.executedOnSenderCausality(7) shouldBe Set()

      dependencyBuilder.calculateDependencies(programOutput6._1, programOutput6._2, programOutput6._3)
      dependencyBuilder.calculateDependencies(programOutput7._1, programOutput7._2, programOutput7._3)

      dependencyBuilder.calculateDependencies(programOutput8._1, programOutput8._2, programOutput8._3)
      //MessageSent(main, mainEnvelope) //8
      dependencyBuilder.executedOnSenderCausality(8) shouldBe Set()
    }

    "calculate executed-on-creator-of-the-sender happens-before constraint correctly" in {
      val dependencyBuilder = new DGB()

      dependencyBuilder.calculateDependencies(programOutput1._1, programOutput1._2, programOutput1._3)
      // ActorCreated(main)
      // ActorCreated(executor)
      // ActorCreated(terminator)
      // ActorCreated(writer),
      // MessageSent(main, executeEnvelope), //1
      // MessageSent(terminator, toTerminatorEnvelope) //2
      // receiver of message 1: main, main is created by: Initial
      dependencyBuilder.executedOnCreatorCausality(1) shouldBe Set()
      // receiver of message 2: terminator, terminator is created by: Initial
      dependencyBuilder.executedOnCreatorCausality(1) shouldBe Set()

      dependencyBuilder.calculateDependencies(programOutput2._1, programOutput2._2, programOutput2._3)
      //MessageSent(writer, writeEnvelope), //3
      //MessageSent(terminator, executionDoneEnvelope) //4
      // receiver of message 3: writer, writer is created by: Initial
      dependencyBuilder.executedOnCreatorCausality(3) shouldBe Set((DGB.EXECUTED_ON_CREATOR, 0))
      // receiver of message 4: terminator, terminator is created by: Initial
      dependencyBuilder.executedOnCreatorCausality(4) shouldBe Set((DGB.EXECUTED_ON_CREATOR, 0))

      // to test transitive predecessors, add more to be executed by terminator (which will create "visitor" later)
      dependencyBuilder.calculateDependencies(programOutput3._1, programOutput3._2, programOutput3._3)

      dependencyBuilder.calculateDependencies(programOutput4._1, programOutput4._2, programOutput4._3)
      // ActorCreated(visitor)
      // MessageSent(writer, flushEnvelope), //5
      // receiver of message 5: writer, writer is created by: Initial
      dependencyBuilder.executedOnCreatorCausality(5) shouldBe Set((DGB.EXECUTED_ON_CREATOR, 0))

      dependencyBuilder.calculateDependencies(programOutput5._1, programOutput5._2, programOutput5._3)
      //MessageSent(terminator, flushedEnvelope) //6
      //MessageSent(visitor, visitorEnvelope) //7
      // receiver of message 6: terminator, terminator is created by: Initial
      dependencyBuilder.executedOnCreatorCausality(6) shouldBe Set((DGB.EXECUTED_ON_CREATOR, 0))
      // receiver of message 7: visitor, visitor is created by: terminator - with actor id 4 - processed before: 2
      dependencyBuilder.executedOnCreatorCausality(7) shouldBe Set((DGB.EXECUTED_ON_CREATOR, 4), (DGB.EXECUTED_ON_CREATOR, 2))

      dependencyBuilder.calculateDependencies(programOutput8._1, programOutput8._2, programOutput8._3)
      //MessageSent(main, mainEnvelope) //8
      // receiver of message 8: main, main is created by: Initial
      dependencyBuilder.executedOnCreatorCausality(8) shouldBe Set((DGB.EXECUTED_ON_CREATOR, 0))
    }

    "calculate sender-receiver happens-before constraint correctly" in {
      val dependencyBuilder = new DGB()

      val senderActor = TestActorRef[Dummy]
      val receiverActor = TestActorRef[Dummy]

      val initEnvelope = Envelope("InitEnvelope", main)
      val envelope1 = Envelope("Envelope-1", senderActor)
      val dummyEnvelope = Envelope("dummy-envelope", senderActor)
      val envelope2 = Envelope("Envelope-1", senderActor)

      /*
        Initial sequence of program events:
        MessageReceived(ActorRef.noSender, Envelope("", ActorRef.noSender)),//0
        ActorCreated(senderActor), ActorCreated(receiverActor), ActorCreated(main),
        MessageSent(senderActor, initEnvelope) //1
      */
      val programOutput1: (Message, List[Message], List[ActorRef]) = (
        Message(0L, ActorRef.noSender, Envelope("", ActorRef.noSender)),
        List(Message(1L, main, executeEnvelope), Message(2L, terminator, toTerminatorEnvelope)),
        List(main, executor, terminator, writer)
      )

      /*
        Next sequence of program events:
        MessageReceived(senderActor, executeEnvelope), //1
        MessageSent(receiverActor, envelope1), //2
        MessageSent(senderActor, dummyEnvelope) //3
      */
      val programOutput2: (Message, List[Message], List[ActorRef]) = (
        Message(1L, senderActor, executeEnvelope),
        List(Message(2L, receiverActor, envelope1), Message(3L, senderActor, dummyEnvelope)),
        List()
      )

      /*
        Next sequence of program events:
        MessageReceived(senderActor, dummyEnvelope), //3
        MessageSent(receiverActor, envelope2) //4
      */
      val programOutput3: (Message, List[Message], List[ActorRef]) = (
        Message(3L, senderActor, dummyEnvelope),
        List(Message(4L, receiverActor, envelope2)),
        List()
      )

      dependencyBuilder.calculateDependencies(programOutput1._1, programOutput1._2, programOutput1._3)
      dependencyBuilder.calculateDependencies(programOutput2._1, programOutput2._2, programOutput2._3)
      dependencyBuilder.calculateDependencies(programOutput3._1, programOutput3._2, programOutput3._3)

      // Sender sends envelope-1 and envelope-2 to the receiver in this order
      // These messages should be causally dependent as they have same sender and receiver
      dependencyBuilder.senderReceiverCausality(4) shouldBe Set((DGB.SENDER_RECEIVER, 2))

      /*
        Next sequence of program events:
        MessageReceived(receiverActor, envelope1), //2
        MessageSent(senderActor, dummyEnvelope) //5
      */
      val programOutput4: (Message, List[Message], List[ActorRef]) = (
        Message(2L, receiverActor, envelope1),
        List(Message(5L, senderActor, dummyEnvelope)),
        List()
      )

      /*
        Next sequence of program events:
        MessageReceived(senderActor, dummyEnvelope), //5
        MessageSent(receiverActor, dummyEnvelope) //6
      */
      val programOutput5: (Message, List[Message], List[ActorRef]) = (
        Message(5L, senderActor, dummyEnvelope),
        List(Message(6L, receiverActor, dummyEnvelope)),
        List()
      )

      dependencyBuilder.calculateDependencies(programOutput4._1, programOutput4._2, programOutput4._3)
      dependencyBuilder.calculateDependencies(programOutput5._1, programOutput5._2, programOutput5._3)

      dependencyBuilder.senderReceiverCausality(6) shouldBe Set((DGB.SENDER_RECEIVER, 4))
    }

    "calculate the predecessors correctly for BITA scenario" in {
      val dependencyBuilder = new DGB()

      // example in Bita paper on Fig1
      /*
        Initial sequence of program events:
        MessageReceived(ActorRef.noSender, Envelope("", ActorRef.noSender)),//0
        ActorCreated(main), ActorCreated(executor), ActorCreated(terminator), ActorCreated(writer),
        MessageSent(main, executeEnvelope) //1
     */
      val programOutput1: (Message, List[Message], List[ActorRef]) = (
        Message(0L, ActorRef.noSender, Envelope("", ActorRef.noSender)),
        List(Message(1L, main, executeEnvelope)),
        List(main, executor, terminator, writer)
      )

      /*
        Next sequence of program events:
        MessageReceived(main, executeEnvelope), //1
        MessageSent(writer, writeEnvelope), //2
        MessageSent(terminator, actionDoneEnvelope) //3
      */
      val programOutput2: (Message, List[Message], List[ActorRef]) = (
        Message(1L, main, executeEnvelope),
        List(Message(2L, writer, writeEnvelope), Message(3L, terminator, actionDoneEnvelope)),
        List()
      )

      /*
        Next sequence of program events:
        MessageReceived(writer, writeEnvelope) //2
      */
      val programOutput3: (Message, List[Message], List[ActorRef]) = (
        Message(2L, writer, writeEnvelope),
        List(),
        List()
      )

      /*
        Next sequence of program events:
        MessageReceived(terminator, actionDoneEnvelope), //3
        MessageSent(writer, flushEnvelope) //4
      */
      val programOutput4: (Message, List[Message], List[ActorRef]) = (
        Message(3L, terminator, actionDoneEnvelope),
        List(Message(4L, writer, flushEnvelope)),
        List()
      )

      /*
        Next sequence of program events:
        MessageReceived(writer, flushEnvelope), //4
        MessageSent(terminator, flushedEnvelope) //5
      */
      val programOutput5: (Message, List[Message], List[ActorRef]) = (
        Message(4L, writer, flushEnvelope),
        List(Message(5L, terminator, flushedEnvelope)),
        List()
      )

      /*
        Next sequence of program events:
        MessageReceived(terminator, flushedEnvelope) //5
      */
      val programOutput6: (Message, List[Message], List[ActorRef]) = (
        Message(5L, terminator, flushedEnvelope),
        List(),
        List()
      )

      dependencyBuilder.calculateDependencies(programOutput1._1, programOutput1._2, programOutput1._3)
      dependencyBuilder.calculateDependencies(programOutput2._1, programOutput2._2, programOutput2._3)
      dependencyBuilder.calculateDependencies(programOutput3._1, programOutput3._2, programOutput3._3)
      dependencyBuilder.calculateDependencies(programOutput4._1, programOutput4._2, programOutput4._3)
      dependencyBuilder.calculateDependencies(programOutput5._1, programOutput5._2, programOutput5._3)
      dependencyBuilder.calculateDependencies(programOutput6._1, programOutput6._2, programOutput6._3)

      // predecessors: in Bita paper on top of right column on p5
      dependencyBuilder.causalityPredecessors(1) shouldBe Set((DGB.MESSAGE_SEND, 0), (DGB.CREATOR, 0)) // pred of "Execute"
      dependencyBuilder.causalityPredecessors(2) shouldBe Set((DGB.MESSAGE_SEND, 1), (DGB.CREATOR, 0), (DGB.EXECUTED_ON_CREATOR, 0)) // pred of "Write" - Set("Execute")
      dependencyBuilder.causalityPredecessors(3) shouldBe Set((DGB.MESSAGE_SEND,1), (DGB.CREATOR,0), (DGB.EXECUTED_ON_CREATOR,0)) // pred of "ActionDone" - Set("Execute")
      dependencyBuilder.causalityPredecessors(4) shouldBe Set((DGB.MESSAGE_SEND,3), (DGB.CREATOR,0), (DGB.EXECUTED_ON_CREATOR,0)) // pred of "Flush" - Set(ActionDone)
      dependencyBuilder.causalityPredecessors(5) shouldBe Set((DGB.MESSAGE_SEND,4), (DGB.CREATOR,0), (DGB.EXECUTED_ON_SENDER,2), (DGB.EXECUTED_ON_CREATOR,0)) // pred of "Flushed" - Set(Flush, Write)
    }

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}

class Dummy extends Actor {
  override def receive: Receive = Actor.emptyBehavior
}