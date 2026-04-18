package app.pages

import app.{ShowcaseCatalog, ShowcaseRoute}
import jfx.action.Button.button
import jfx.control.Image.{alt, image, src}
import jfx.core.component.CompositeComponent
import jfx.core.component.CompositeComponent.composite
import jfx.core.component.ElementComponent.*
import jfx.dsl.*
import jfx.dsl.Scope.inject
import jfx.layout.Div.div
import jfx.layout.HBox.hbox
import jfx.router.Router
import org.scalajs.dom.HTMLDivElement

class HomePage extends CompositeComponent[HTMLDivElement] {

  override val element: HTMLDivElement = newElement("div")

  override protected def compose(using CompositeComponent.DslContext): Unit =
    withDslContext {
      given HomePage = this

      classes = "clarity-page clarity-page--home"

      style {
        display = "flex"
        flexDirection = "column"
        gap = "28px"
        maxWidth = "1240px"
        margin = "0 auto"
      }

      div {
        classes = "home-hero"

        image {
          classes = "home-hero__image"
          src = "/scalajs-jfx/og-image.svg"
          alt = ""
        }

        div {
          classes = "home-hero__content"

          div {
            classes = "home-eyebrow"
            text = "Scala.js UI architecture"
          }

          div {
            classes = "home-hero__title"
            text = "Typed browser apps that keep their shape as they grow."
          }

          div {
            classes = "home-hero__copy"
            text =
              "scalajs-jfx brings a JavaFX-inspired component model to Scala.js: routed pages, typed form state, remote data views, secondary windows and live reference material all follow one composable DSL."
          }

          hbox {
            classes = "clarity-action-row home-hero__actions"

            button("Start with forms") {
              classes = Seq("calm-action", "calm-action--primary")
              onClick { _ =>
                inject[Router].navigate("/form")
              }
            }

            button("See the data flow") {
              classes = Seq("calm-action", "calm-action--secondary")
              onClick { _ =>
                inject[Router].navigate("/table")
              }
            }

            button("Open reference") {
              classes = Seq("calm-action", "calm-action--quiet")
              onClick { _ =>
                inject[Router].navigate("/docs")
              }
            }
          }
        }

        div {
          classes = "home-hero__metrics"

          metric("01", "One component model", "Pages, forms, lists and overlays are built with the same primitives.")
          metric("02", "Typed state flow", "Inputs and data views stay close to Scala models instead of DOM glue.")
          metric("03", "Guided demos", "Each workspace answers a concrete architecture question.")
        }
      }

      div {
        classes = "home-section home-section--intro"

        sectionHeading(
          label = "Architecture",
          title = "A small set of primitives carries the whole application.",
          copy = "The project is easiest to understand as a chain: routing creates the workspace, components own precise state, data surfaces react to changes, and the viewport keeps secondary work in context."
        )

        div {
          classes = "home-architecture-flow"

          architectureStep(
            index = "Route",
            title = "Choose a workspace",
            body = "Navigation maps directly to focused pages, so each workflow has a stable entry point."
          )

          architectureStep(
            index = "State",
            title = "Bind typed models",
            body = "Fields, lists and selections move through Scala values before they touch the DOM."
          )

          architectureStep(
            index = "Render",
            title = "Compose the surface",
            body = "Reusable components create forms, tables and reference examples from the same DSL."
          )

          architectureStep(
            index = "Viewport",
            title = "Keep context alive",
            body = "Dialogs, windows and notifications support side tasks without breaking the route."
          )
        }
      }

      div {
        classes = "home-section"

        sectionHeading(
          label = "Value",
          title = "The benefits are practical: less glue, clearer boundaries, safer UI state.",
          copy = "scalajs-jfx is useful when a browser app needs real structure without giving up the speed and reach of Scala.js."
        )

        div {
          classes = "home-benefit-grid"

          benefitCard(
            title = "Readable growth",
            body = "Features can expand from a single component into routed workspaces without switching mental models."
          )

          benefitCard(
            title = "Typed user input",
            body = "Forms stay close to Scala domain objects, including nested structures and media fields."
          )

          benefitCard(
            title = "Honest async surfaces",
            body = "Loading, filtering, sorting and selection are treated as first-class UI states."
          )

          benefitCard(
            title = "Side work without chaos",
            body = "Windows and notifications make supporting tasks visible while the primary flow remains intact."
          )
        }
      }

      div {
        classes = "home-section"

        sectionHeading(
          label = "Guided demos",
          title = "Open the workspace that matches the question in your head.",
          copy = "The demos now read as a tour through the architecture instead of isolated raw samples."
        )

        div {
          classes = "home-demo-grid"

          demoCard(
            route = ShowcaseCatalog.formWorkspace,
            step = "First stop",
            title = "Model-driven forms",
            body = "Start here when you want to understand typed fields, nested subforms, validation surfaces, media editing and revision history."
          )

          demoCard(
            route = ShowcaseCatalog.dataQueue,
            step = "Data layer",
            title = "Remote lists and tables",
            body = "Use this workspace for loading states, filtering, sorting, row selection and detail panes under realistic pressure."
          )

          demoCard(
            route = ShowcaseCatalog.windowWorkspace,
            step = "Secondary work",
            title = "Viewport windows",
            body = "See how supporting tasks, notifications and detachable panes stay connected to the active route."
          )

          demoCard(
            route = ShowcaseCatalog.referenceAtlas,
            step = "Reference",
            title = "Component patterns",
            body = "Jump here for import paths, compact usage notes and live examples that match the rest of the app."
          )
        }
      }

      div {
        classes = "home-section home-section--closing"

        div {
          classes = "home-closing__copy"

          div {
            classes = "home-eyebrow"
            text = "Project purpose"
          }

          div {
            classes = "home-closing__title"
            text = "A precise Scala UI toolkit for applications that need structure before they need spectacle."
          }

          div {
            classes = "home-closing__body"
            text =
              "The repository is both framework and proof: a compact runtime, a coherent demo app and a living reference that show how Scala.js can carry serious browser interfaces with architectural calm."
          }
        }

        button("Explore the reference") {
          classes = Seq("calm-action", "calm-action--primary")
          onClick { _ =>
            inject[Router].navigate("/docs")
          }
        }
      }
    }

  private def sectionHeading(label: String, title: String, copy: String): Unit =
    div {
      classes = "home-section-heading"

      div {
        classes = "home-eyebrow"
        text = label
      }

      div {
        classes = "home-section-heading__title"
        text = title
      }

      div {
        classes = "home-section-heading__copy"
        text = copy
      }
    }

  private def metric(index: String, title: String, body: String): Unit =
    div {
      classes = "home-metric"

      div {
        classes = "home-metric__index"
        text = index
      }

      div {
        classes = "home-metric__title"
        text = title
      }

      div {
        classes = "home-metric__body"
        text = body
      }
    }

  private def architectureStep(index: String, title: String, body: String): Unit =
    div {
      classes = "home-architecture-step"

      div {
        classes = "home-architecture-step__index"
        text = index
      }

      div {
        classes = "home-architecture-step__title"
        text = title
      }

      div {
        classes = "home-architecture-step__body"
        text = body
      }
    }

  private def benefitCard(title: String, body: String): Unit =
    div {
      classes = "home-benefit-card"

      div {
        classes = "home-benefit-card__title"
        text = title
      }

      div {
        classes = "home-benefit-card__body"
        text = body
      }
    }

  private def demoCard(route: ShowcaseRoute, step: String, title: String, body: String)(using CompositeComponent.DslContext): Unit =
    div {
      classes = "home-demo-card"

      div {
        classes = "home-demo-card__meta"
        text = step
      }

      div {
        classes = "home-demo-card__title"
        text = title
      }

      div {
        classes = "home-demo-card__body"
        text = body
      }

      button("Open " + route.title) {
        classes = Seq("calm-action", "calm-action--secondary")
        onClick { _ =>
          inject[Router].navigate(route.path)
        }
      }
    }
}

object HomePage {
  def homePage(init: HomePage ?=> Unit = {}): HomePage =
    composite(new HomePage())
}
