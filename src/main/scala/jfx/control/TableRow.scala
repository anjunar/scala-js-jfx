package jfx.control

import jfx.core.component.NativeComponent
import jfx.core.state.{Disposable, Property}
import org.scalajs.dom.HTMLDivElement

class TableRow[S] extends NativeComponent[HTMLDivElement] {

  private final class CellSlot(
    val column: TableColumn[S, Any],
    val cell: TableCell[S, Any]
  ) {
    private var valueObserver: Disposable = TableRow.noopDisposable

    def bind(
      tableView: TableView[S],
      row: TableRow[S],
      rowValue: S,
      rowIndex: Int,
      selected: Boolean,
      lastColumn: Boolean
    ): Unit = {
      valueObserver.dispose()
      cell.setColumnWidth(column.effectiveWidth, lastColumn)
      cell.applyContext(tableView, row, column, rowIndex, selected)
      val observableValue = column.resolveCellValue(tableView, rowValue, rowIndex)
      if (observableValue == null) {
        cell.applyRenderedItem(null, empty = false)
        valueObserver = TableRow.noopDisposable
      } else {
        valueObserver = observableValue.observe(value => cell.applyRenderedItem(value, empty = false))
      }
    }

    def clear(): Unit = {
      valueObserver.dispose()
      valueObserver = TableRow.noopDisposable
      cell.applyContext(null, null, column, -1, selected = false)
      cell.applyRenderedItem(null, empty = true)
    }

    def showPlaceholder(
      tableView: TableView[S],
      row: TableRow[S],
      rowIndex: Int,
      lastColumn: Boolean
    ): Unit = {
      valueObserver.dispose()
      valueObserver = TableRow.noopDisposable
      cell.setColumnWidth(column.effectiveWidth, lastColumn)
      cell.applyContext(tableView, row, column, rowIndex, selected = false)
      cell.applyRenderedItem(null, empty = true)
    }

    def disposeBinding(): Unit = {
      valueObserver.dispose()
      valueObserver = TableRow.noopDisposable
    }
  }

  val itemProperty: Property[S | Null] = Property(null)
  val emptyProperty: Property[Boolean] = Property(true)
  val selectedProperty: Property[Boolean] = Property(false)
  val indexProperty: Property[Int] = Property(-1)
  val tableViewProperty: Property[TableView[S] | Null] = Property(null)

  private var cellSlots: Vector[CellSlot] = Vector.empty

  override lazy val element: HTMLDivElement = {
    val div = newElement("div")
    div.className = "jfx-table-row"
    div.style.position = "absolute"
    div.style.left = "0"
    div.style.display = "flex"
    div.style.boxSizing = "border-box"
    div.style.cursor = "default"
    div.style.setProperty("user-select", "none")
    div.onclick = _ => {
      val tableView = tableViewProperty.get
      if (tableView != null && !emptyProperty.get) {
        tableView.getSelectionModel.select(indexProperty.get)
      }
    }
    div
  }

  def getItem: S | Null = itemProperty.get
  def getIndex: Int = indexProperty.get
  def isEmpty: Boolean = emptyProperty.get
  def isSelected: Boolean = selectedProperty.get
  def getTableView: TableView[S] | Null = tableViewProperty.get

  protected def updateItem(item: S | Null, empty: Boolean): Unit = ()

  protected def updateSelected(selected: Boolean): Unit = {
    element.style.backgroundColor =
      if (selected) "#dbeafe"
      else if (emptyProperty.get) "transparent"
      else if (math.abs(indexProperty.get) % 2 == 0) "#ffffff"
      else "#f8fafc"
  }

  private[control] def rebuildCells(columns: Seq[TableColumn[S, ?]]): Unit = {
    cellSlots.foreach(_.disposeBinding())
    cellSlots = Vector.empty
    clearChildren()
    columns.foreach { column =>
      val typedColumn = column.asInstanceOf[TableColumn[S, Any]]
      val cell = typedColumn.createCell().asInstanceOf[TableCell[S, Any]]
      cellSlots = cellSlots :+ CellSlot(typedColumn, cell)
      addChild(cell)
    }
  }

  private[control] def bind(
    rowIndex: Int,
    rowValue: S,
    tableView: TableView[S],
    columns: Seq[TableColumn[S, ?]],
    rowHeight: Double,
    rowWidth: Double
  ): Unit = {
    if (cellSlots.length != columns.length) rebuildCells(columns)

    val selected = tableView.getSelectionModel.getSelectedIndex == rowIndex

    element.style.display = "flex"
    element.style.top = s"${rowIndex * rowHeight}px"
    element.style.height = s"${rowHeight}px"
    element.style.width = s"${math.max(0.0, rowWidth)}px"

    tableViewProperty.set(tableView)
    indexProperty.set(rowIndex)
    itemProperty.set(rowValue)
    emptyProperty.set(false)
    selectedProperty.set(selected)

    updateItem(rowValue, empty = false)
    updateSelected(selected)

    cellSlots.zipWithIndex.foreach { case (slot, index) =>
      slot.bind(
        tableView = tableView,
        row = this,
        rowValue = rowValue,
        rowIndex = rowIndex,
        selected = selected,
        lastColumn = index == cellSlots.length - 1
      )
    }
  }

  private[control] def clear(rowHeight: Double, rowWidth: Double): Unit = {
    element.style.display = "none"
    element.style.height = s"${rowHeight}px"
    element.style.width = s"${math.max(0.0, rowWidth)}px"

    tableViewProperty.set(null)
    indexProperty.set(-1)
    itemProperty.set(null)
    emptyProperty.set(true)
    selectedProperty.set(false)

    updateItem(null, empty = true)
    updateSelected(selected = false)

    cellSlots.foreach(_.clear())
  }

  private[control] def showPlaceholder(
    rowIndex: Int,
    tableView: TableView[S],
    columns: Seq[TableColumn[S, ?]],
    rowHeight: Double,
    rowWidth: Double
  ): Unit = {
    if (cellSlots.length != columns.length) rebuildCells(columns)

    element.style.display = "flex"
    element.style.top = s"${rowIndex * rowHeight}px"
    element.style.height = s"${rowHeight}px"
    element.style.width = s"${math.max(0.0, rowWidth)}px"

    tableViewProperty.set(tableView)
    indexProperty.set(rowIndex)
    itemProperty.set(null)
    emptyProperty.set(true)
    selectedProperty.set(false)

    updateItem(null, empty = true)
    updateSelected(selected = false)

    cellSlots.zipWithIndex.foreach { case (slot, index) =>
      slot.showPlaceholder(
        tableView = tableView,
        row = this,
        rowIndex = rowIndex,
        lastColumn = index == cellSlots.length - 1
      )
    }
  }

  override def dispose(): Unit = {
    cellSlots.foreach(_.disposeBinding())
    cellSlots = Vector.empty
    super.dispose()
  }
}

object TableRow {
  private val noopDisposable: Disposable = () => ()
}
