package jfx.form.editor.plugins

import jfx.action.Button
import jfx.core.component.CompositeComponent
import jfx.core.component.ElementComponent.*
import jfx.core.state.Property
import jfx.core.state.Property.subscribeBidirectional
import jfx.dsl.*
import jfx.form.Input
import jfx.layout.{Div, HBox, VBox, Viewport}
import lexical.{COMMAND_PRIORITY, ImageModule, ImageNode, ImagePayload, LexicalEditor, ToolbarElement}
import org.scalajs.dom.{Event, FileReader, HTMLDivElement, HTMLImageElement, HTMLInputElement, document}

import scala.scalajs.js

final case class ImageDialogResult(
  src: String,
  alt: String | Null,
  title: String | Null,
  widthPx: Int
)

class ImagePlugin extends AbstractEditorPlugin("image-plugin") {

  override val name: String = "image"

  var dialogTitle: String = "Bild einfuegen"
  var defaultWidthPx: Int = 680
  var previewMaxHeightPx: Int = 320
  var widthPresets: Seq[Int] = Seq(320, 560, 760)

  override val toolbarElements: Seq[ToolbarElement] =
    Seq(new ImageModule())

  override val nodes: Seq[js.Any] =
    Seq(js.constructorOf[ImageNode])

  override def install(editor: LexicalEditor): js.Function0[Unit] =
    editor.registerCommand(
      ImageNode.OPEN_IMAGE_DIALOG_COMMAND,
      (_: LexicalEditor, _: LexicalEditor) => {
        openImageEditor(editor)
        true
      },
      COMMAND_PRIORITY.EDITOR
    )

  private def openImageEditor(editor: LexicalEditor): Unit = {
    given Scope = currentPluginScope

    Viewport.addWindow(
      new Viewport.WindowConf(
        title = dialogTitle,
        width = 760,
        height = 760,
        component = Viewport.captureComponent {
          CompositeComponent.composite(
            new ImageDialog(
              defaultWidthPx = defaultWidthPx,
              previewMaxHeightPx = previewMaxHeightPx,
              widthPresets = widthPresets,
              saveImage = result => insertImage(editor, result)
            )
          )
        }
      )
    )
  }

  private def insertImage(editor: LexicalEditor, result: ImageDialogResult): Unit = {
    val payload =
      js.Dynamic
        .literal(
          src = result.src,
          altText = Option(result.alt).map(_.trim).filter(_.nonEmpty).orNull,
          maxWidth = result.widthPx
        )
        .asInstanceOf[ImagePayload]

    editor.dispatchCommand(ImageNode.INSERT_IMAGE_COMMAND, payload)
  }
}

object ImagePlugin {

  def imagePlugin(init: ImagePlugin ?=> Unit = {}): ImagePlugin =
    PluginFactory.build(new ImagePlugin())(init)
}

private final class ImageDialog(
  defaultWidthPx: Int,
  previewMaxHeightPx: Int,
  widthPresets: Seq[Int],
  saveImage: ImageDialogResult => Unit
) extends CompositeComponent[HTMLDivElement]
    with Viewport.CloseAware {

  protected type DslContext = CompositeComponent.DslContext

  override val element: HTMLDivElement = newElement("div")

  private val srcProperty = Property("")
  private val altProperty = Property("")
  private val titleProperty = Property("")
  private val widthProperty = Property("")
  private val statusProperty = Property("")
  private val previewDimensionProperty = Property("")

  private var closeWindow: () => Unit = () => ()
  private var previewImage: HTMLImageElement | Null = null
  private var previewPlaceholder: HTMLDivElement | Null = null
  private var naturalWidthPx: Int | Null = null
  private var widthTouched = false

  addDisposable(srcProperty.observe(_ => syncPreview()))
  addDisposable(widthProperty.observeWithoutInitial(_ => widthTouched = true))

  override def close_=(callback: () => Unit): Unit =
    closeWindow = callback

  override protected def compose(using DslContext): Unit = {
    classProperty += "image-plugin-dialog"

    withDslContext {
      VBox.vbox {
        classes = "image-plugin-dialog__shell"
        style {
          rowGap = "14px"
          width = "100%"
          height = "100%"
        }

        Div.div {
          classes = "image-plugin-dialog__intro"
          text = "Fuege ein Bild per Datei oder URL ein. Bilder werden im Editor zentriert und responsiv dargestellt."
        }

        Div.div {
          classes = "image-plugin-dialog__preview-shell"

          val previewRoot = summon[Div].element

          previewPlaceholder = document.createElement("div").asInstanceOf[HTMLDivElement]
          previewPlaceholder.nn.className = "image-plugin-dialog__preview-placeholder"
          previewPlaceholder.nn.textContent = "Noch kein Bild gewählt"

          previewImage = document.createElement("img").asInstanceOf[HTMLImageElement]
          val previewImg = previewImage.nn
          previewImg.className = "image-plugin-dialog__preview-image"
          previewImg.style.display = "none"
          previewImg.style.maxHeight = s"${previewMaxHeightPx}px"
          previewImg.onload = (_: Event) => syncPreviewMetrics()
          previewImg.addEventListener(
            "error",
            (_: Event) => {
              naturalWidthPx = null
              previewDimensionProperty.set("")
              statusProperty.set("Bild konnte nicht geladen werden.")
              syncPreview()
            }
          )

          previewRoot.appendChild(previewPlaceholder.nn)
          previewRoot.appendChild(previewImg)
        }

        Div.div {
          classes = "image-plugin-dialog__meta"
          style {
            display <-- previewDimensionProperty.map(value => if (value.trim.nonEmpty) "block" else "none")
          }
          subscribeBidirectional(previewDimensionProperty, textProperty)
        }

        Div.div {
          classes = "image-plugin-dialog__section-title"
          text = "Quelle"
        }

        Div.div {
          classes = "image-plugin-dialog__field-group"

          Input.input("src") {
            val current = summon[Input]
            current.classProperty += "image-plugin-dialog__input"
            current.placeholder = "https://... oder data:image/..."
            subscribeBidirectional(srcProperty, current.stringValueProperty)
          }

          HBox.hbox {
            classes = "image-plugin-dialog__actions-row"
            style {
              columnGap = "10px"
            }

            val fileInputHost = Div.div {
              classes = "image-plugin-dialog__file-host"
            }

            val fileInput = document.createElement("input").asInstanceOf[HTMLInputElement]
            fileInput.`type` = "file"
            fileInput.accept = "image/*"
            fileInput.className = "image-plugin-dialog__file-input"
            fileInput.onchange = (_: Event) => {
              val selectedFile = Option(fileInput.files).flatMap(files => Option(files.item(0))).orNull

              if (selectedFile != null) {
                val reader = new FileReader()
                reader.onload = (_: Event) => {
                  val encoded =
                    Option(reader.result)
                      .map(_.toString)
                      .map(_.trim)
                      .filter(_.nonEmpty)

                  encoded.foreach { dataUrl =>
                    srcProperty.set(dataUrl)
                    statusProperty.set("")

                    if (titleProperty.get.trim.isEmpty) {
                      titleProperty.set(fileNameWithoutExtension(selectedFile.name))
                    }
                    if (altProperty.get.trim.isEmpty) {
                      altProperty.set(fileNameWithoutExtension(selectedFile.name))
                    }
                  }
                }
                reader.readAsDataURL(selectedFile)
              }
            }
            fileInputHost.element.appendChild(fileInput)

            Button.button("Datei waehlen") {
              val current = summon[Button]
              current.buttonType = "button"
              current.classProperty.setAll(Seq("image-plugin-dialog__button", "image-plugin-dialog__button--secondary"))
              current.addClick { _ =>
                fileInput.click()
              }
            }

            Button.button("URL leeren") {
              val current = summon[Button]
              current.buttonType = "button"
              current.classProperty.setAll(Seq("image-plugin-dialog__button", "image-plugin-dialog__button--ghost"))
              current.addClick { _ =>
                srcProperty.set("")
                statusProperty.set("")
              }
            }
          }
        }

        Div.div {
          classes = "image-plugin-dialog__section-title"
          text = "Darstellung"
        }

        Div.div {
          classes = "image-plugin-dialog__field-group"

          Input.input("width") {
            val current = summon[Input]
            current.classProperty += "image-plugin-dialog__input"
            current.placeholder = "Breite in px"
            current.element.`type` = "number"
            subscribeBidirectional(widthProperty, current.stringValueProperty)
          }

          HBox.hbox {
            classes = "image-plugin-dialog__preset-row"
            style {
              columnGap = "8px"
              flexWrap = "wrap"
            }

            widthPresets.foreach { preset =>
              Button.button(s"${preset}px") {
                val current = summon[Button]
                current.buttonType = "button"
                current.classProperty.setAll(Seq("image-plugin-dialog__chip"))
                current.addClick { _ =>
                  widthProperty.set(preset.toString)
                  statusProperty.set("")
                }
              }
            }

            Button.button("Original") {
              val current = summon[Button]
              current.buttonType = "button"
              current.classProperty.setAll(Seq("image-plugin-dialog__chip"))
              current.addClick { _ =>
                if (naturalWidthPx != null) {
                  widthProperty.set(naturalWidthPx.nn.toString)
                }
              }
            }
          }
        }

        Div.div {
          classes = "image-plugin-dialog__section-title"
          text = "Metadaten"
        }

        Div.div {
          classes = "image-plugin-dialog__field-group"

          Input.input("alt") {
            val current = summon[Input]
            current.classProperty += "image-plugin-dialog__input"
            current.placeholder = "Alternativtext"
            subscribeBidirectional(altProperty, current.stringValueProperty)
          }

          Input.input("title") {
            val current = summon[Input]
            current.classProperty += "image-plugin-dialog__input"
            current.placeholder = "Titel / Tooltip"
            subscribeBidirectional(titleProperty, current.stringValueProperty)
          }
        }

        Div.div {
          classes = Seq("image-plugin-dialog__meta", "image-plugin-dialog__meta--error")
          style {
            display <-- statusProperty.map(value => if (value.trim.nonEmpty) "block" else "none")
          }
          subscribeBidirectional(statusProperty, textProperty)
        }

        HBox.hbox {
          classes = "image-plugin-dialog__footer"
          style {
            columnGap = "10px"
            justifyContent = "flex-end"
          }

          Button.button("Einsetzen") {
            val current = summon[Button]
            current.buttonType = "button"
            current.classProperty.setAll(Seq("image-plugin-dialog__button", "image-plugin-dialog__button--primary"))
            current.addClick { _ =>
              saveCurrentImage()
            }
          }
        }
      }
    }

    syncPreview()
  }

  private def syncPreview(): Unit = {
    val src = srcProperty.get.trim

    if (previewImage == null || previewPlaceholder == null) return

    if (src.nonEmpty) {
      previewImage.nn.src = src
      previewImage.nn.style.display = "block"
      previewPlaceholder.nn.style.display = "none"
    } else {
      previewImage.nn.removeAttribute("src")
      previewImage.nn.style.display = "none"
      previewPlaceholder.nn.style.display = "flex"
      previewDimensionProperty.set("")
      naturalWidthPx = null
    }
  }

  private def syncPreviewMetrics(): Unit = {
    if (previewImage == null) return

    val width = previewImage.nn.naturalWidth
    val height = previewImage.nn.naturalHeight

    naturalWidthPx = width
    previewDimensionProperty.set(s"${width}px x ${height}px")
    statusProperty.set("")

    if (!widthTouched && widthProperty.get.trim.isEmpty) {
      widthProperty.set(math.min(width, defaultWidthPx).toString)
    }
  }

  private def saveCurrentImage(): Unit = {
    val src = srcProperty.get.trim
    if (src.isEmpty) {
      statusProperty.set("Bitte waehle eine Bildquelle.")
      return
    }

    val resolvedWidth =
      widthProperty.get.trim.toIntOption
        .filter(_ > 0)
        .orElse(Option(naturalWidthPx).map(width => math.min(width, defaultWidthPx)))
        .getOrElse(defaultWidthPx)

    saveImage(
      ImageDialogResult(
        src = src,
        alt = blankToNull(altProperty.get),
        title = blankToNull(titleProperty.get),
        widthPx = resolvedWidth
      )
    )
    closeWindow()
  }

  private def blankToNull(value: String): String | Null =
    Option(value).map(_.trim).filter(_.nonEmpty).orNull

  private def fileNameWithoutExtension(value: String): String =
    Option(value)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map { name =>
        val lastDot = name.lastIndexOf('.')
        if (lastDot > 0) name.substring(0, lastDot) else name
      }
      .getOrElse("Bild")
}