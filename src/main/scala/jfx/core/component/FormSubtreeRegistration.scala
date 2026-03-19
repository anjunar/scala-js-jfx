package jfx.core.component

import jfx.form.{ArrayForm, Control, Formular, Model}
import org.scalajs.dom.{HTMLFieldSetElement, Node}

trait FormSubtreeRegistration { self: NodeComponent[? <: Node] =>

  protected def enclosingFormOption(): Option[Formular[?,?]] =
    this match {
      case arrayForm : ArrayForm[?] => Some(new Formular[?, HTMLFieldSetElement] {
        override val name: String = arrayForm.name
        override lazy val element: HTMLFieldSetElement = arrayForm.element
      })
      case form: Formular[?,?] => Some(form)
      case _ => findParentFormOption()
    }

  protected final def registerSubtree(component: NodeComponent[? <: Node]): Unit =
    enclosingFormOption().foreach(form => registerSubtree(component, form))

  protected final def unregisterSubtree(component: NodeComponent[? <: Node]): Unit =
    enclosingFormOption().foreach(form => unregisterSubtree(component, form))

  private def registerSubtree(component: NodeComponent[? <: Node], form: Formular[?,?]): Unit = {
    component match {
      case control: Control[?, ?] => form.addControl(control)
      case _ => ()
    }

    component match {
      case children: ChildrenComponent[?] =>
        children.childrenProperty.foreach(child => registerSubtree(child, form))
      case _ => ()
    }
  }

  private def unregisterSubtree(component: NodeComponent[? <: Node], form: Formular[?,?]): Unit = {
    component match {
      case control: Control[?, ?] => form.removeControl(control)
      case _ => ()
    }

    component match {
      case children: ChildrenComponent[?] =>
        children.childrenProperty.foreach(child => unregisterSubtree(child, form))
      case _ => ()
    }
  }
}
