import com.google.inject.AbstractModule
import controllers.{ChildHelloActor, HelloActor}
import play.api.libs.concurrent.AkkaGuiceSupport

class MyModule extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindActor[HelloActor]("hello-actor")
    bindActor[ChildHelloActor]("child-hello-actor")
  }
}
