package jfx.form.editor.plugins

import lexical.{LexicalEditor, LexicalList, ListModules, ToolbarElement}

import scala.scalajs.js

class ListPlugin extends AbstractEditorPlugin("list-plugin") {

  override val name: String = "list"

  override val toolbarElements: Seq[ToolbarElement] =
    Seq(ListModules.BULLET, ListModules.NUMBERED)

  override val nodes: Seq[js.Any] =
    Seq(LexicalList.ListNode, LexicalList.ListItemNode)

  override def install(editor: LexicalEditor): js.Function0[Unit] =
    LexicalList.registerList(editor)
}

object ListPlugin {

  def listPlugin(init: ListPlugin ?=> Unit = {}): ListPlugin =
    PluginFactory.build(new ListPlugin())(init)
}
