package app

import org.scalajs.dom.{document, window}

object Theme {

  enum Mode(val id: String, val metaColor: String) {
    case Light extends Mode("light", "#eee9e1")
    case Dark extends Mode("dark", "#171918")
  }

  private val storageKey = "scalajs-jfx.theme"

  def initialMode(): Mode =
    readStoredMode().getOrElse(systemMode())

  def apply(mode: Mode): Unit = {
    document.documentElement.setAttribute("data-theme", mode.id)
    setThemeColor(mode.metaColor)
    writeStoredMode(mode)
  }

  def toggle(mode: Mode): Mode =
    mode match {
      case Mode.Light => Mode.Dark
      case Mode.Dark  => Mode.Light
    }

  def iconName(mode: Mode): String =
    mode match {
      case Mode.Light => "dark_mode"
      case Mode.Dark  => "light_mode"
    }

  def label(mode: Mode): String =
    mode match {
      case Mode.Light => "Switch to dark theme"
      case Mode.Dark  => "Switch to light theme"
    }

  def buttonLabel(mode: Mode): String =
    mode match {
      case Mode.Light => "Dark"
      case Mode.Dark  => "Light"
    }

  private def readStoredMode(): Option[Mode] =
    try
      Option(window.localStorage.getItem(storageKey)).flatMap {
        case "light" => Some(Mode.Light)
        case "dark"  => Some(Mode.Dark)
        case _       => None
      }
    catch {
      case _: Throwable => None
    }

  private def writeStoredMode(mode: Mode): Unit =
    try window.localStorage.setItem(storageKey, mode.id)
    catch {
      case _: Throwable => ()
    }

  private def systemMode(): Mode =
    try
      if window.matchMedia("(prefers-color-scheme: dark)").matches then Mode.Dark
      else Mode.Light
    catch {
      case _: Throwable => Mode.Light
    }

  private def setThemeColor(value: String): Unit = {
    val element = Option(document.head.querySelector("""meta[name="theme-color"]"""))
      .getOrElse {
        val meta = document.createElement("meta")
        meta.setAttribute("name", "theme-color")
        document.head.appendChild(meta)
        meta
      }
      .asInstanceOf[org.scalajs.dom.HTMLMetaElement]

    element.content = value
  }
}
