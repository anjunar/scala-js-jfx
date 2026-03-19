package jfx.form

import jfx.core.component.ElementComponent
import jfx.core.state.Property
import org.scalajs.dom.HTMLElement

trait Control[V, E <: HTMLElement] extends ElementComponent[E] {
  
  val name : String
  
  val valueProperty : Property[V]

}
