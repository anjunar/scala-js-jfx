package jfx.control

import jfx.core.state.{CompositeDisposable, Disposable, Property, ReadOnlyProperty}

class TableColumn[S, T](initialText: String = "") {

  val textProperty: Property[String] = Property(initialText)
  val prefWidthProperty: Property[Double] = Property(160.0)
  val minWidthProperty: Property[Double] = Property(40.0)
  val maxWidthProperty: Property[Double] = Property(Double.PositiveInfinity)
  val sortableProperty: Property[Boolean] = Property(false)
  val resizableProperty: Property[Boolean] = Property(true)
  val cellValueFactoryProperty: Property[TableColumn.CellDataFeatures[S, T] => ReadOnlyProperty[T] | Null] =
    Property(null)
  val cellFactoryProperty: Property[TableColumn[S, T] => TableCell[S, T] | Null] =
    Property(null)

  def getText: String = textProperty.get
  def setText(value: String): Unit = textProperty.set(if (value == null) "" else value)
  def text: String = getText
  def text_=(value: String): Unit = setText(value)

  def getPrefWidth: Double = prefWidthProperty.get
  def setPrefWidth(value: Double): Unit = prefWidthProperty.set(value)
  def prefWidth: Double = getPrefWidth
  def prefWidth_=(value: Double): Unit = setPrefWidth(value)

  def getMinWidth: Double = minWidthProperty.get
  def setMinWidth(value: Double): Unit = minWidthProperty.set(value)

  def getMaxWidth: Double = maxWidthProperty.get
  def setMaxWidth(value: Double): Unit = maxWidthProperty.set(value)

  def isSortable: Boolean = sortableProperty.get
  def setSortable(value: Boolean): Unit = sortableProperty.set(value)

  def isResizable: Boolean = resizableProperty.get
  def setResizable(value: Boolean): Unit = resizableProperty.set(value)

  def getCellValueFactory: TableColumn.CellDataFeatures[S, T] => ReadOnlyProperty[T] | Null =
    cellValueFactoryProperty.get

  def setCellValueFactory(factory: TableColumn.CellDataFeatures[S, T] => ReadOnlyProperty[T] | Null): Unit =
    cellValueFactoryProperty.set(factory)

  def getCellFactory: TableColumn[S, T] => TableCell[S, T] | Null =
    cellFactoryProperty.get

  def setCellFactory(factory: TableColumn[S, T] => TableCell[S, T] | Null): Unit =
    cellFactoryProperty.set(factory)

  private[control] def effectiveWidth: Double = {
    val maxWidth = maxWidthProperty.get
    val minWidth = minWidthProperty.get
    val preferredWidth = prefWidthProperty.get
    val boundedMax = math.max(minWidth, maxWidth)
    math.max(minWidth, math.min(boundedMax, preferredWidth))
  }

  private[control] def resolveCellValue(
    tableView: TableView[S],
    rowValue: S,
    rowIndex: Int
  ): ReadOnlyProperty[T] | Null = {
    val factory = cellValueFactoryProperty.get
    if (factory == null) null
    else factory(TableColumn.CellDataFeatures(tableView, this, rowValue, rowIndex))
  }

  private[control] def createCell(): TableCell[S, T] = {
    val factory = cellFactoryProperty.get
    if (factory == null) new TableCell[S, T]()
    else {
      val cell = factory(this)
      if (cell == null) new TableCell[S, T]() else cell
    }
  }

  private[control] def observeColumnState(listener: () => Unit): Disposable = {
    val composite = new CompositeDisposable()
    composite.add(observeWithoutInitial(textProperty)(_ => listener()))
    composite.add(observeWithoutInitial(prefWidthProperty)(_ => listener()))
    composite.add(observeWithoutInitial(minWidthProperty)(_ => listener()))
    composite.add(observeWithoutInitial(maxWidthProperty)(_ => listener()))
    composite.add(observeWithoutInitial(sortableProperty)(_ => listener()))
    composite.add(observeWithoutInitial(resizableProperty)(_ => listener()))
    composite.add(observeWithoutInitial(cellValueFactoryProperty)(_ => listener()))
    composite.add(observeWithoutInitial(cellFactoryProperty)(_ => listener()))
    composite
  }

  private def observeWithoutInitial[V](property: Property[V])(listener: V => Unit): Disposable = {
    var first = true
    property.observe { value =>
      if (first) first = false
      else listener(value)
    }
  }
}

object TableColumn {

  final case class CellDataFeatures[S, T](
    tableView: TableView[S],
    tableColumn: TableColumn[S, T],
    value: S,
    index: Int
  ) {
    def getTableView: TableView[S] = tableView
    def getTableColumn: TableColumn[S, T] = tableColumn
    def getValue: S = value
    def getIndex: Int = index
  }
}
