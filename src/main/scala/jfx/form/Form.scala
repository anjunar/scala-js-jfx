package jfx.form

import jfx.core.component.{ChildrenComponent, ElementComponent, NativeComponent, NodeComponent}
import jfx.core.state.{ListProperty, Property}
import jfx.core.state.ListProperty.*
import org.scalajs.dom.{Event, HTMLElement, HTMLFormElement, Node}

class Form[M <: Model[M]](model : M) extends NativeComponent[HTMLFormElement], Formular[M, HTMLFormElement] {

  override val name: String = "default"
  
  valueProperty.asInstanceOf[Property[M]].set(model)

  private var submitHandler: Event => Unit = _ => ()
  
  lazy val element: HTMLFormElement = {
    val formElement = newElement("form")
    formElement.onsubmit = event => {
      event.preventDefault()
      submitHandler(event)
    }
    formElement
  }

  def onSubmit: Event => Unit = submitHandler

  def onSubmit_=(listener: Event => Unit): Unit =
    submitHandler = if (listener == null) _ => () else listener
  
}
