package jfx.core.component

import org.scalajs.dom.Node

/**
 * Marker trait: if a component mixes this in, its subtree will not be registered into any enclosing
 * `jfx.form.Formular`.
 */
trait FormRegistrationBoundary { self: NodeComponent[? <: Node] => }

