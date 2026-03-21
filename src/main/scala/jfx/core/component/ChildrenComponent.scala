package jfx.core.component

import jfx.core.component.ElementComponent
import jfx.core.state.ListProperty
import org.scalajs.dom.{HTMLElement, Node, console}

trait ChildrenComponent[E <: Node] extends ElementComponent[E] {

  val childrenProperty: ListProperty[NodeComponent[? <: Node]] =
    new ListProperty[NodeComponent[? <: Node]]()

  override def dispose(): Unit = {
    val children = childrenProperty.toList
    childrenProperty.clear()
    children.foreach(_.dispose())
    disposable.dispose()
  }

  def addChild(child: NodeComponent[? <: Node]): Unit = {
    if (childrenProperty.contains(child)) {
      console.warn("Child already in list")
      return
    }
    childrenProperty += child
  }

  def removeChild(child: NodeComponent[? <: Node]): Unit =
    childrenProperty -= child

  def insertChild(index: Int, child: NodeComponent[? <: Node]): Unit =
    if (childrenProperty.contains(child)) {
      console.warn("Child already in list")
      return
    }
    childrenProperty.insert(index, child)

  def clearChildren(): Unit =
    childrenProperty.clear()

  override private[jfx] def attachChild(child: NodeComponent[? <: Node]): Unit =
    addChild(child)

  override private[jfx] def detachChild(child: NodeComponent[? <: Node]): Boolean = {
    val containsChild = childrenProperty.contains(child)
    if (containsChild) {
      removeChild(child)
    }
    containsChild
  }

  override private[jfx] def childComponentsIterator: Iterator[NodeComponent[? <: Node]] =
    childrenProperty.iterator


}
