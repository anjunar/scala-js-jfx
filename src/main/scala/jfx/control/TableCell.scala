package jfx.control

import jfx.core.component.NativeComponent
import jfx.core.state.Property
import org.scalajs.dom.HTMLDivElement

class TableCell[S, T] extends NativeComponent[HTMLDivElement] {

  val itemProperty: Property[T | Null] = Property(null)
  val emptyProperty: Property[Boolean] = Property(true)
  val selectedProperty: Property[Boolean] = Property(false)
  val indexProperty: Property[Int] = Property(-1)
  val tableViewProperty: Property[TableView[S] | Null] = Property(null)
  val tableRowProperty: Property[TableRow[S] | Null] = Property(null)
  val tableColumnProperty: Property[TableColumn[S, T] | Null] = Property(null)

  override lazy val element: HTMLDivElement = {
    val div = newElement("div")
    div.className = "jfx-table-cell"
    div.style.boxSizing = "border-box"
    div.style.display = "flex"
    div.style.setProperty("align-items", "center")
    div.style.padding = "0 10px"
    div.style.overflow = "hidden"
    div.style.whiteSpace = "nowrap"
    div.style.textOverflow = "ellipsis"
    div.style.borderBottom = "1px solid #e5e7eb"
    div.style.borderRight = "1px solid #e5e7eb"
    div.style.backgroundColor = "inherit"
    div
  }

  def getItem: T | Null = itemProperty.get
  def getIndex: Int = indexProperty.get
  def isEmpty: Boolean = emptyProperty.get
  def isSelected: Boolean = selectedProperty.get
  def getTableView: TableView[S] | Null = tableViewProperty.get
  def getTableRow: TableRow[S] | Null = tableRowProperty.get
  def getTableColumn: TableColumn[S, T] | Null = tableColumnProperty.get

  protected def updateItem(item: T | Null, empty: Boolean): Unit =
    textContent = if (empty || item == null) "" else item.toString

  protected def updateSelected(selected: Boolean): Unit = ()

  private[control] def applyContext(
    tableView: TableView[S] | Null,
    tableRow: TableRow[S] | Null,
    tableColumn: TableColumn[S, T] | Null,
    index: Int,
    selected: Boolean
  ): Unit = {
    tableViewProperty.set(tableView)
    tableRowProperty.set(tableRow)
    tableColumnProperty.set(tableColumn)
    indexProperty.set(index)
    selectedProperty.set(selected)
    updateSelected(selected)
  }

  private[control] def applyRenderedItem(item: T | Null, empty: Boolean): Unit = {
    itemProperty.set(item)
    emptyProperty.set(empty)
    updateItem(item, empty)
  }

  private[control] def setColumnWidth(width: Double, lastColumn: Boolean): Unit = {
    val boundedWidth = math.max(0.0, width)
    val widthValue = s"${boundedWidth}px"
    element.style.setProperty("flex", s"0 0 $widthValue")
    element.style.width = widthValue
    element.style.minWidth = widthValue
    element.style.maxWidth = widthValue
    element.style.borderRight = if (lastColumn) "none" else "1px solid #e5e7eb"
  }
}
