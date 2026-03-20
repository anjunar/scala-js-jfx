package jfx.control

import jfx.core.component.{ElementComponent, FormSubtreeRegistration, NodeComponent}
import jfx.core.state.{CompositeDisposable, Disposable, ListProperty, Property}
import org.scalajs.dom.{Event, HTMLDivElement, Node, window}

import scala.scalajs.js

class TableView[S] extends ElementComponent[HTMLDivElement], FormSubtreeRegistration {

  private val headerViewport: HTMLDivElement = newElement("div")
  private val headerContent: HTMLDivElement = newElement("div")
  private val bodyWrapper: HTMLDivElement = newElement("div")
  private val viewport: HTMLDivElement = newElement("div")
  private val content: HTMLDivElement = newElement("div")
  private val placeholderLayer: HTMLDivElement = newElement("div")
  private val defaultPlaceholder: HTMLDivElement = newElement("div")

  private val itemsRefProperty: Property[ListProperty[S]] = Property(new ListProperty[S]())
  val columnsProperty: ListProperty[TableColumn[S, ?]] = new ListProperty[TableColumn[S, ?]]()
  val fixedCellSizeProperty: Property[Double] = Property(28.0)
  val placeholderProperty: Property[NodeComponent[? <: Node] | Null] = Property(null)
  val rowFactoryProperty: Property[TableView[S] => TableRow[S]] = Property(_ => new TableRow[S]())

  private val selectionModel = new TableViewSelectionModel(this)

  private var disposed = false
  private var scheduledRefresh = false
  private var itemsObserver: Disposable = TableView.noopDisposable
  private var columnObserver: Disposable = TableView.noopDisposable
  private var rowPool: Vector[TableRow[S]] = Vector.empty
  private var mountedPlaceholder: NodeComponent[? <: Node] | Null = null

  override lazy val element: HTMLDivElement = {
    val div = newElement("div")
    div.className = "jfx-table-view"
    div.style.display = "flex"
    div.style.setProperty("flex-direction", "column")
    div.style.position = "relative"
    div.style.overflow = "hidden"
    div.style.boxSizing = "border-box"
    div.style.border = "1px solid #cbd5e1"
    div.style.borderRadius = "6px"
    div.style.backgroundColor = "#ffffff"
    div.style.minWidth = "0"
    div.style.minHeight = "0"
    div.style.height = "400px"
    div
  }

  initializeStructure()
  initializeDefaultPlaceholder()
  initializeObservers()

  def itemsProperty: Property[ListProperty[S]] = itemsRefProperty
  def getItems: ListProperty[S] = itemsRefProperty.get
  def setItems(items: ListProperty[S]): Unit =
    itemsRefProperty.set(if (items == null) new ListProperty[S]() else items)

  def items: ListProperty[S] = getItems
  def items_=(items: ListProperty[S]): Unit = setItems(items)

  def getColumns: ListProperty[TableColumn[S, ?]] = columnsProperty

  def getFixedCellSize: Double = fixedCellSizeProperty.get
  def setFixedCellSize(value: Double): Unit = fixedCellSizeProperty.set(value)

  def getPlaceholder: NodeComponent[? <: Node] | Null = placeholderProperty.get
  def setPlaceholder(value: NodeComponent[? <: Node] | Null): Unit = placeholderProperty.set(value)

  def getRowFactory: TableView[S] => TableRow[S] = rowFactoryProperty.get
  def setRowFactory(factory: TableView[S] => TableRow[S]): Unit =
    rowFactoryProperty.set(if (factory == null) (_ => new TableRow[S]()) else factory)

  def getSelectionModel: TableViewSelectionModel[S] = selectionModel

  def refresh(): Unit = refreshNow()

  def scrollTo(index: Int): Unit = {
    if (disposed) return
    val itemCount = getItems.length
    if (itemCount == 0) return
    val clamped = math.max(0, math.min(itemCount - 1, index))
    viewport.scrollTop = clamped * effectiveRowHeight
    refreshVisibleRows()
  }

  override def onMount(): Unit = scheduleRefresh()

  override def dispose(): Unit = {
    if (disposed) return
    disposed = true

    itemsObserver.dispose()
    columnObserver.dispose()
    detachMountedPlaceholder()
    hideDefaultPlaceholder()
    disposeRows()

    super.dispose()
  }

  private def initializeStructure(): Unit = {
    headerViewport.className = "jfx-table-header-viewport"
    headerViewport.style.position = "relative"
    headerViewport.style.overflow = "hidden"
    headerViewport.style.setProperty("flex", "0 0 auto")
    headerViewport.style.borderBottom = "1px solid #cbd5e1"
    headerViewport.style.backgroundColor = "#f8fafc"

    headerContent.className = "jfx-table-header-content"
    headerContent.style.display = "flex"
    headerContent.style.position = "relative"
    headerContent.style.left = "0"
    headerContent.style.top = "0"
    headerContent.style.minHeight = "100%"

    bodyWrapper.className = "jfx-table-body-wrapper"
    bodyWrapper.style.position = "relative"
    bodyWrapper.style.setProperty("flex", "1 1 auto")
    bodyWrapper.style.minHeight = "0"
    bodyWrapper.style.minWidth = "0"
    bodyWrapper.style.overflow = "hidden"
    bodyWrapper.style.backgroundColor = "#ffffff"

    viewport.className = "jfx-table-viewport"
    viewport.style.position = "relative"
    viewport.style.height = "100%"
    viewport.style.width = "100%"
    viewport.style.overflow = "auto"
    viewport.style.minHeight = "0"
    viewport.style.minWidth = "0"
    viewport.style.backgroundColor = "#ffffff"

    content.className = "jfx-table-content"
    content.style.position = "relative"
    content.style.minHeight = "100%"
    content.style.backgroundColor = "#ffffff"

    placeholderLayer.className = "jfx-table-placeholder"
    placeholderLayer.style.position = "absolute"
    placeholderLayer.style.setProperty("inset", "0")
    placeholderLayer.style.display = "none"
    placeholderLayer.style.setProperty("align-items", "center")
    placeholderLayer.style.setProperty("justify-content", "center")
    placeholderLayer.style.padding = "16px"
    placeholderLayer.style.boxSizing = "border-box"
    placeholderLayer.style.backgroundColor = "rgba(255,255,255,0.92)"

    headerViewport.appendChild(headerContent)
    viewport.appendChild(content)
    bodyWrapper.appendChild(viewport)
    bodyWrapper.appendChild(placeholderLayer)
    element.appendChild(headerViewport)
    element.appendChild(bodyWrapper)
  }

  private def initializeDefaultPlaceholder(): Unit = {
    defaultPlaceholder.className = "jfx-table-default-placeholder"
    defaultPlaceholder.textContent = "No content in table"
    defaultPlaceholder.style.color = "#64748b"
    defaultPlaceholder.style.fontSize = "14px"
    defaultPlaceholder.style.textAlign = "center"
  }

  private def initializeObservers(): Unit = {
    val scrollListener: js.Function1[Event, Unit] = _ => {
      syncHeaderScroll()
      refreshVisibleRows()
    }
    viewport.addEventListener("scroll", scrollListener)
    disposable.add(() => viewport.removeEventListener("scroll", scrollListener))

    val resizeHandling = installResizeHandling()
    disposable.add(resizeHandling)
    disposable.add(selectionModel)

    val itemsRefObserver = itemsRefProperty.observe { _ =>
      rewireItemsObserver()
      scheduleRefresh()
    }
    disposable.add(itemsRefObserver)

    val columnsObserver = columnsProperty.observeChanges { _ =>
      onColumnsChanged()
    }
    disposable.add(columnsObserver)

    val fixedCellObserver = fixedCellSizeProperty.observe { _ =>
      scheduleRefresh()
    }
    disposable.add(fixedCellObserver)

    val placeholderObserver = placeholderProperty.observe { _ =>
      refreshPlaceholder()
    }
    disposable.add(placeholderObserver)

    val rowFactoryObserver = rowFactoryProperty.observe { _ =>
      disposeRows()
      scheduleRefresh()
    }
    disposable.add(rowFactoryObserver)

    val selectionObserver = selectionModel.selectedIndexProperty.observe { _ =>
      refreshVisibleRows()
    }
    disposable.add(selectionObserver)

    rewireItemsObserver()
    onColumnsChanged()
    scheduleRefresh()
    scheduleInitialRefresh(60)
  }

  private def rewireItemsObserver(): Unit = {
    itemsObserver.dispose()
    itemsObserver = getItems.observeChanges { _ =>
      scheduleRefresh()
    }
  }

  private def onColumnsChanged(): Unit = {
    columnObserver.dispose()

    val composite = new CompositeDisposable()
    currentColumns.foreach { column =>
      composite.add(column.observeColumnState(() => onColumnsChanged()))
    }
    columnObserver = composite

    rebuildHeader()
    rowPool.foreach(_.rebuildCells(currentColumns))
    scheduleRefresh()
  }

  private def refreshNow(): Unit = {
    if (disposed) return

    val columns = currentColumns
    val rowHeight = effectiveRowHeight
    val headerHeight = effectiveHeaderHeight(rowHeight)
    val itemCount = getItems.length
    val contentWidth = updateLayoutMetrics(columns, rowHeight, headerHeight, itemCount)

    refreshPlaceholder()
    refreshVisibleRows(columns = columns, rowHeight = rowHeight, rowWidth = contentWidth)
    syncHeaderScroll()
  }

  private def refreshVisibleRows(): Unit =
    refreshVisibleRows(
      columns = currentColumns,
      rowHeight = effectiveRowHeight,
      rowWidth = currentContentWidth
    )

  private def refreshVisibleRows(
    columns: Seq[TableColumn[S, Any]],
    rowHeight: Double,
    rowWidth: Double
  ): Unit = {
    if (disposed) return

    val items = getItems
    val itemCount = items.length
    if (itemCount == 0) {
      ensureRowPool(0, columns)
      return
    }

    val viewportHeight = math.max(viewport.clientHeight.toDouble, 0.0)
    val baseVisibleCount = math.max(1, math.ceil(viewportHeight / rowHeight).toInt)
    val requiredRows = math.min(itemCount, baseVisibleCount + TableView.overscanRows * 2)
    ensureRowPool(requiredRows, columns)

    val firstVisibleIndex = math.floor(viewport.scrollTop / rowHeight).toInt
    val startIndex =
      if (requiredRows >= itemCount) 0
      else math.max(0, math.min(itemCount - requiredRows, firstVisibleIndex - TableView.overscanRows))

    rowPool.zipWithIndex.foreach { case (row, poolIndex) =>
      val rowIndex = startIndex + poolIndex
      if (rowIndex < itemCount) {
        row.bind(
          rowIndex = rowIndex,
          rowValue = items(rowIndex),
          tableView = this,
          columns = columns,
          rowHeight = rowHeight,
          rowWidth = rowWidth
        )
      } else {
        row.clear(rowHeight, rowWidth)
      }
    }
  }

  private def ensureRowPool(requiredRows: Int, columns: Seq[TableColumn[S, Any]]): Unit = {
    while (rowPool.length < requiredRows) {
      val row = createRow(columns)
      content.appendChild(row.element)
      row.parent = Some(this)
      row.onMount()
      registerSubtree(row)
      rowPool = rowPool :+ row
    }

    while (rowPool.length > requiredRows) {
      val row = rowPool.last
      rowPool = rowPool.dropRight(1)
      unregisterSubtree(row)
      row.parent = None
      removeDomNode(row.element)
      row.dispose()
    }
  }

  private def createRow(columns: Seq[TableColumn[S, Any]]): TableRow[S] = {
    val factory = rowFactoryProperty.get
    val row =
      if (factory == null) new TableRow[S]()
      else {
        val created = factory(this)
        if (created == null) new TableRow[S]() else created
      }
    row.rebuildCells(columns)
    row
  }

  private def disposeRows(): Unit = {
    rowPool.foreach { row =>
      unregisterSubtree(row)
      row.parent = None
      removeDomNode(row.element)
      row.dispose()
    }
    rowPool = Vector.empty
  }

  private def rebuildHeader(): Unit = {
    removeAllChildren(headerContent)

    currentColumns.zipWithIndex.foreach { case (column, index) =>
      val cell = newElement("div")
      cell.className = "jfx-table-header-cell"
      cell.textContent = column.getText
      cell.style.display = "flex"
      cell.style.setProperty("align-items", "center")
      cell.style.boxSizing = "border-box"
      cell.style.padding = "0 10px"
      cell.style.fontWeight = "600"
      cell.style.fontSize = "13px"
      cell.style.color = "#1e293b"
      cell.style.whiteSpace = "nowrap"
      cell.style.overflow = "hidden"
      cell.style.textOverflow = "ellipsis"
      cell.style.borderRight = if (index == currentColumns.length - 1) "none" else "1px solid #cbd5e1"
      val widthValue = s"${column.effectiveWidth}px"
      cell.style.setProperty("flex", s"0 0 $widthValue")
      cell.style.width = widthValue
      cell.style.minWidth = widthValue
      cell.style.maxWidth = widthValue
      headerContent.appendChild(cell)
    }
  }

  private def updateLayoutMetrics(
    columns: Seq[TableColumn[S, Any]],
    rowHeight: Double,
    headerHeight: Double,
    itemCount: Int
  ): Double = {
    val viewportWidth = math.max(viewport.clientWidth.toDouble, 0.0)
    val columnWidth = columns.foldLeft(0.0)(_ + _.effectiveWidth)
    val contentWidth = math.max(columnWidth, viewportWidth)
    val contentHeight = math.max(0.0, itemCount * rowHeight)

    headerViewport.style.height = s"${headerHeight}px"
    headerContent.style.height = s"${headerHeight}px"
    headerContent.style.width = s"${contentWidth}px"
    headerContent.style.minWidth = s"${contentWidth}px"

    content.style.width = s"${contentWidth}px"
    content.style.minWidth = s"${contentWidth}px"
    content.style.height = s"${contentHeight}px"

    contentWidth
  }

  private def refreshPlaceholder(): Unit = {
    val showPlaceholder = getItems.isEmpty
    placeholderLayer.style.display = if (showPlaceholder) "flex" else "none"

    if (!showPlaceholder) {
      detachMountedPlaceholder()
      hideDefaultPlaceholder()
      return
    }

    val customPlaceholder = placeholderProperty.get
    if (customPlaceholder == null) {
      detachMountedPlaceholder()
      showDefaultPlaceholder()
    } else {
      hideDefaultPlaceholder()
      mountPlaceholder(customPlaceholder)
    }
  }

  private def mountPlaceholder(placeholder: NodeComponent[? <: Node]): Unit = {
    if (mountedPlaceholder == placeholder && placeholder.element.parentNode == placeholderLayer) return

    detachMountedPlaceholder()
    removeAllChildren(placeholderLayer)

    placeholderLayer.appendChild(placeholder.element)
    mountedPlaceholder = placeholder
    placeholder.parent = Some(this)
    placeholder.onMount()
    registerSubtree(placeholder)
  }

  private def detachMountedPlaceholder(): Unit = {
    val placeholder = mountedPlaceholder
    if (placeholder != null) {
      unregisterSubtree(placeholder)
      if (placeholder.parent.contains(this)) placeholder.parent = None
      removeDomNode(placeholder.element)
      mountedPlaceholder = null
    }
  }

  private def showDefaultPlaceholder(): Unit = {
    if (defaultPlaceholder.parentNode != placeholderLayer) {
      removeAllChildren(placeholderLayer)
      placeholderLayer.appendChild(defaultPlaceholder)
    }
  }

  private def hideDefaultPlaceholder(): Unit = {
    if (defaultPlaceholder.parentNode == placeholderLayer) {
      placeholderLayer.removeChild(defaultPlaceholder)
    }
  }

  private def syncHeaderScroll(): Unit = {
    headerContent.style.transform = s"translateX(${-viewport.scrollLeft}px)"
  }

  private def scheduleRefresh(): Unit = {
    if (disposed || scheduledRefresh) return
    scheduledRefresh = true
    window.requestAnimationFrame { _ =>
      scheduledRefresh = false
      refreshNow()
    }
  }

  private def scheduleInitialRefresh(remainingFrames: Int): Unit = {
    if (disposed || remainingFrames <= 0) return
    window.requestAnimationFrame { _ =>
      if (!disposed) {
        refreshNow()
        if (!element.isConnected || viewport.clientHeight == 0) {
          scheduleInitialRefresh(remainingFrames - 1)
        }
      }
    }
  }

  private def installResizeHandling(): Disposable = {
    val composite = new CompositeDisposable()

    val listener: js.Function1[Event, Unit] = _ => scheduleRefresh()
    window.addEventListener("resize", listener)
    composite.add(() => window.removeEventListener("resize", listener))

    val resizeObserverCtor = js.Dynamic.global.ResizeObserver
    if (!js.isUndefined(resizeObserverCtor) && resizeObserverCtor != null) {
      val callback: js.Function2[js.Any, js.Any, Unit] = (_, _) => scheduleRefresh()
      val observer = js.Dynamic.newInstance(resizeObserverCtor)(callback)
      observer.observe(element)
      observer.observe(bodyWrapper)
      composite.add(() => observer.disconnect())
    }

    composite
  }

  private def effectiveRowHeight: Double = {
    val configured = fixedCellSizeProperty.get
    if (configured > 0) configured else 28.0
  }

  private def effectiveHeaderHeight(rowHeight: Double): Double =
    math.max(30.0, rowHeight)

  private def currentColumns: Vector[TableColumn[S, Any]] =
    columnsProperty.iterator.map(_.asInstanceOf[TableColumn[S, Any]]).toVector

  private def currentContentWidth: Double = {
    val viewportWidth = math.max(viewport.clientWidth.toDouble, 0.0)
    math.max(currentColumns.foldLeft(0.0)(_ + _.effectiveWidth), viewportWidth)
  }

  private def removeAllChildren(node: Node): Unit = {
    var maybeChild = node.firstChild
    while (maybeChild != null) {
      val child = maybeChild.asInstanceOf[Node]
      maybeChild = child.nextSibling
      node.removeChild(child)
    }
  }

  private def removeDomNode(node: Node): Unit = {
    val parent = node.parentNode
    if (parent != null) parent.removeChild(node)
  }
}

object TableView {
  private[control] val overscanRows = 6
  private[control] val noopDisposable: Disposable = () => ()
}

class TableViewSelectionModel[S](tableView: TableView[S]) extends Disposable {

  val selectedIndexProperty: Property[Int] = Property(-1)
  val selectedItemProperty: Property[S | Null] = Property(null)

  private var itemsObserver: Disposable = TableView.noopDisposable

  private val itemsRefObserver = tableView.itemsProperty.observe { items =>
    itemsObserver.dispose()
    itemsObserver = items.observeChanges { _ =>
      reconcile()
    }
    reconcile()
  }

  def getSelectedIndex: Int = selectedIndexProperty.get
  def getSelectedItem: S | Null = selectedItemProperty.get

  def clearSelection(): Unit = {
    selectedIndexProperty.set(-1)
    selectedItemProperty.set(null)
  }

  def select(index: Int): Unit = {
    val items = tableView.getItems
    if (index < 0 || index >= items.length) clearSelection()
    else {
      selectedIndexProperty.set(index)
      selectedItemProperty.set(items(index))
    }
  }

  def select(item: S): Unit = {
    val index = tableView.getItems.indexOf(item)
    if (index < 0) clearSelection()
    else select(index)
  }

  private def reconcile(): Unit = {
    val items = tableView.getItems
    val index = selectedIndexProperty.get
    if (index < 0 || index >= items.length) clearSelection()
    else selectedItemProperty.set(items(index))
  }

  override def dispose(): Unit = {
    itemsObserver.dispose()
    itemsRefObserver.dispose()
  }
}
