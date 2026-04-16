package jfx.form.editor.plugins

import lexical.{EditorModules, ToolbarElement, RedoModule, UndoModule}

class BasePlugin extends AbstractEditorPlugin("base-plugin") {

  override val name: String = "base"

  override val toolbarElements: Seq[ToolbarElement] =
    Seq(
      new UndoModule(),
      new RedoModule(),
      EditorModules.BOLD,
      EditorModules.ITALIC
    )
}

object BasePlugin {

  def basePlugin(init: BasePlugin ?=> Unit = {}): BasePlugin =
    PluginFactory.build(new BasePlugin())(init)
}
