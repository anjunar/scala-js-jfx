package jfx.form.editor.plugins

import jfx.core.component.NodeComponent
import jfx.dsl.Scope
import lexical.{EditorModule, LexicalEditor, ToolbarElement}
import org.scalajs.dom.Node

import scala.scalajs.js

trait EditorPlugin { self: NodeComponent[? <: Node] =>

  def name: String

  def toolbarElements: Seq[ToolbarElement] = Seq.empty

  def modules: Seq[EditorModule] = Seq.empty

  def nodes: Seq[js.Any] = Seq.empty

  def install(editor: LexicalEditor): js.Function0[Unit] =
    () => ()

  private[jfx] def captureScope(scope: Scope): Unit
}
