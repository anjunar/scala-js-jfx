package jfx.form.editor.plugins

import lexical.{HeadingDropdown, LexicalRichText, ToolbarElement}

import scala.scalajs.js

class HeadingPlugin extends AbstractEditorPlugin("heading-plugin") {

  override val name: String = "heading"

  override val toolbarElements: Seq[ToolbarElement] =
    Seq(new HeadingDropdown())

  override val nodes: Seq[js.Any] =
    Seq(LexicalRichText.HeadingNode)
}

object HeadingPlugin {

  def headingPlugin(init: HeadingPlugin ?=> Unit = {}): HeadingPlugin =
    PluginFactory.build(new HeadingPlugin())(init)
}
