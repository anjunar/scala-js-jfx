package jfx.core.component

import jfx.core.state.Property
import org.scalajs.dom.Node

trait Component[E <: Node] extends NodeComponent[E] {

  val textContentProperty = new Property[String]("")

  def newElement(tag: String): E = org.scalajs.dom.document.createElement(tag).asInstanceOf[E]
  
  private val textContentObserver = textContentProperty.observe { text => element.textContent = text }

  def textContent: String = textContentProperty.get

  def textContent_=(value: String): Unit = textContentProperty.set(value)


}
