package jfx.router

import jfx.core.component.NodeComponent
import jfx.dsl.Scope
import org.scalajs.dom.Node

import scala.scalajs.js

case class RouteContext(
  path: String,
  url: String,
  fullPath: String,
  pathParams: js.Map[String, String],
  queryParams: js.Map[String, String],
  state: RouterState,
  routeMatch: RouteMatch
)

object RouteContext {
  def routeContext(using context: RouteContext): RouteContext =
    context
}

case class Route(
  path: String,
  factory: Route.Factory,
  children: js.Array[Route] = js.Array()
)

object Route {
  trait AwaitSyntax {
    extension [A](inline thenable: js.Thenable[A])
      transparent inline def await: A =
        js.await(js.Promise.resolve(thenable))
  }

  private object AwaitSyntax extends AwaitSyntax

  type Component = NodeComponent[? <: Node] | Null
  type Factory = (RouteContext, Scope) => js.Promise[Component]
  type ScopedFactory = RouteContext ?=> Scope ?=> AwaitSyntax ?=> Component
  type PromiseScopedFactory = RouteContext ?=> Scope ?=> js.Promise[Component]

  transparent inline def scoped(
    path: String,
    inline factory: ScopedFactory,
    children: js.Array[Route] = js.Array()
  ): Route =
    new Route(
      path,
      (context, scope) => js.async[Component] {
        factory(using context)(using scope)(using AwaitSyntax)
      },
      children
    )

  def scopedPromise(
    path: String,
    factory: PromiseScopedFactory,
    children: js.Array[Route] = js.Array()
  ): Route =
    new Route(path, (context, scope) => factory(using context)(using scope), children)
}
