package app

import jfx.dsl.*
import jfx.router.Route

import scala.scalajs.js

object Routes {

  val routes = js.Array[Route](

    Route.scoped(
      path = "/",
      factory = {
        val greeting = js.Promise.resolve("Hallo Welt!").await
        div {
          text = greeting
        }
      },
      children = js.Array(
        Route.scoped(
          path = "/person",
          factory = {
            val context = routeContext
            div {
              text = s"Person ${context.queryParams.get("id").getOrElse("")}".trim
            }
          }
        )
      )
    )

  )

}
