package jfx.form.editor.plugins

import jfx.core.component.ManagedElementComponent
import jfx.dsl.{ComponentContext, DslRuntime, Scope}
import org.scalajs.dom.HTMLDivElement

abstract class AbstractEditorPlugin(cssClass: String)
    extends ManagedElementComponent[HTMLDivElement]
    with EditorPlugin {

  override val element: HTMLDivElement = {
    val divElement = newElement("div")
    if (cssClass != null && cssClass.trim.nonEmpty) {
      divElement.classList.add(cssClass)
    }
    divElement.style.display = "none"
    divElement
  }

  private var scope: Scope = Scope.root()

  private[jfx] override final def captureScope(nextScope: Scope): Unit =
    scope = nextScope

  protected final def currentPluginScope: Scope =
    scope

  protected final def withPluginContext[A](block: Scope ?=> A): A =
    DslRuntime.withComponentContext(ComponentContext(Some(this), findParentFormOption())) {
      given Scope = scope
      block
    }
}
