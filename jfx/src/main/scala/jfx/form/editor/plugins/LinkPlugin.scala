package jfx.form.editor.plugins

import lexical.{LexicalLink, LinkModule, ToolbarElement}

import scala.scalajs.js

class LinkPlugin extends AbstractEditorPlugin("link-plugin") {

  override val name: String = "link"

  override val toolbarElements: Seq[ToolbarElement] =
    Seq(new LinkModule())

  override val nodes: Seq[js.Any] =
    Seq(LexicalLink.LinkNode)
}

object LinkPlugin {

  def linkPlugin(init: LinkPlugin ?=> Unit = {}): LinkPlugin =
    PluginFactory.build(new LinkPlugin())(init)
}
