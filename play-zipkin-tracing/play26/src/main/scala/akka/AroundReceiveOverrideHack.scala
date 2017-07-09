package akka

import akka.actor.Actor

trait AroundReceiveOverrideHack extends Actor {

  override protected[akka] def aroundReceive(receive: Receive, msg: Any): Unit = {
    aroundReceiveMessage(receive, msg)
  }

  protected def aroundReceiveMessage(receive: Receive, msg: Any): Unit = super.aroundReceive(receive, msg)

}