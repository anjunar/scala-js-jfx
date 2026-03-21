package jfx.layout

import jfx.core.component.CompositeComponent
import jfx.core.state.Property
import jfx.dsl.DslRuntime
import org.scalajs.dom.{Event, HTMLDivElement, KeyboardEvent, window}

final class Drawer(slot: Drawer ?=> Unit = {}) extends CompositeComponent[HTMLDivElement] {

  val openProperty: Property[Boolean] = Property(false)
  val widthProperty: Property[String] = Property("280px")
  val closeOnScrimClickProperty: Property[Boolean] = Property(true)

  private val scrim = new Div()
  private val panelShell = new Div()
  private val panel = new Div()
  private val navigationHost = new Div()
  private val contentHost = new Div()

  private var activeDslContext: CompositeComponent.DslContext | Null = null
  private var structureInitialized = false

  override lazy val element: HTMLDivElement = {
    val divElement = newElement("div")
    divElement.classList.add("jfx-drawer")
    divElement
  }

  private val openObserver = openProperty.observe(syncOpenState)
  addDisposable(openObserver)

  private val widthObserver = widthProperty.observe { width =>
    css.setProperty("--drawer-width", width)
  }
  addDisposable(widthObserver)

  private val scrimClickListener: Event => Unit = _ => {
    if (closeOnScrimClickProperty.get && openProperty.get) {
      close()
    }
  }
  scrim.element.addEventListener("click", scrimClickListener)
  addDisposable(() => scrim.element.removeEventListener("click", scrimClickListener))

  private val keyDownListener: KeyboardEvent => Unit = event => {
    if (event.key == "Escape" && openProperty.get) {
      close()
    }
  }
  window.addEventListener("keydown", keyDownListener)
  addDisposable(() => window.removeEventListener("keydown", keyDownListener))

  override protected def compose(using CompositeComponent.DslContext): Unit =
    withDslContext {
      given Drawer = this
      initializeStructure()
      activeDslContext = summon[CompositeComponent.DslContext]
      try slot
      finally activeDslContext = null
    }

  def isOpen: Boolean =
    openProperty.get

  def isOpen_=(value: Boolean): Unit =
    openProperty.set(value)

  def width: String =
    widthProperty.get

  def width_=(value: String): Unit =
    widthProperty.set(value)

  def closeOnScrimClick: Boolean =
    closeOnScrimClickProperty.get

  def closeOnScrimClick_=(value: Boolean): Unit =
    closeOnScrimClickProperty.set(value)

  def open(): Unit =
    openProperty.set(true)

  def close(): Unit =
    openProperty.set(false)

  def toggle(): Unit =
    openProperty.set(!openProperty.get)

  def navigation(init: => Unit): Unit =
    withSection(navigationHost)(init)

  def content(init: => Unit): Unit =
    withSection(contentHost)(init)

  private def initializeStructure()(using CompositeComponent.DslContext): Unit =
    if (!structureInitialized) {
      structureInitialized = true

      scrim.classProperty += "jfx-drawer__scrim"
      panelShell.classProperty += "jfx-drawer__panel-shell"
      panel.classProperty += "jfx-drawer__panel"
      navigationHost.classProperty += "jfx-drawer__navigation"
      contentHost.classProperty += "jfx-drawer__content"

      addChild(scrim)
      addChild(panelShell)
      addChild(contentHost)

      panelShell.addChild(panel)
      panel.addChild(navigationHost)
    }

  private def syncOpenState(isOpen: Boolean): Unit =
    if (isOpen) {
      element.classList.add("jfx-drawer--open")
    } else {
      element.classList.remove("jfx-drawer--open")
    }

  private def withSection(host: Div)(init: => Unit): Unit = {
    val context = activeDslContext
    if (context == null) {
      throw IllegalStateException("Drawer sections can only be declared while the drawer is composing")
    }

    DslRuntime.withCompositeContext(host, context) {
      given CompositeComponent.DslContext = context
      given Div = host
      init
    }
  }

}

object Drawer {

  def drawer(init: Drawer ?=> Unit = {}): Drawer =
    CompositeComponent.composite(new Drawer(init))

  def drawerNavigation(init: => Unit)(using drawer: Drawer): Unit =
    drawer.navigation(init)

  def drawerContent(init: => Unit)(using drawer: Drawer): Unit =
    drawer.content(init)

  def drawerOpen(using drawer: Drawer): Boolean =
    drawer.isOpen

  def drawerOpen_=(value: Boolean)(using drawer: Drawer): Unit =
    drawer.isOpen = value

  def drawerWidth(using drawer: Drawer): String =
    drawer.width

  def drawerWidth_=(value: String)(using drawer: Drawer): Unit =
    drawer.width = value

  def closeOnScrimClick(using drawer: Drawer): Boolean =
    drawer.closeOnScrimClick

  def closeOnScrimClick_=(value: Boolean)(using drawer: Drawer): Unit =
    drawer.closeOnScrimClick = value

  def openDrawer(using drawer: Drawer): Unit =
    drawer.open()

  def closeDrawer(using drawer: Drawer): Unit =
    drawer.close()

  def toggleDrawer(using drawer: Drawer): Unit =
    drawer.toggle()
}
