package com.ivy.wallet.ui.theme.components

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivy.legacy.utils.numberBetween
import com.ivy.legacy.utils.swap
import com.ivy.ui.R
import com.ivy.wallet.domain.data.Reorderable
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalSave
import java.util.UUID

private val TitleInset = 24.dp
private val RowOuterHorizontal = 16.dp
private val RowOuterVertical = 4.dp
private val RowMinHeight = 56.dp
private val BadgeSize = 40.dp
private val DraggingElevation = 6.dp

/** Default sheet header: "Reorder" plus a one-line hint on how to drag. */
@Composable
private fun ColumnScope.DefaultReorderTitle() {
    Text(
        modifier = Modifier.padding(horizontal = TitleInset),
        text = stringResource(R.string.reorder),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        modifier = Modifier.padding(horizontal = TitleInset),
        text = stringResource(R.string.reorder_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Standard content for a reorder row: an optional coloured icon badge and the item's name.
 * Takes the remaining width so the drag handle stays at the end of the row.
 */
@Composable
fun RowScope.ReorderItemLabel(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    iconName: String? = null,
    @DrawableRes defaultIcon: Int? = null,
) {
    Row(
        modifier = modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (accentColor != null) {
            Box(
                modifier = Modifier
                    .size(BadgeSize)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center,
            ) {
                if (defaultIcon != null) {
                    ItemIconSDefaultIcon(
                        iconName = iconName,
                        defaultIcon = defaultIcon,
                        tint = findContrastTextColor(accentColor),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
        }

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("UNCHECKED_CAST", "ParameterNaming", "FunctionParameterNaming")
@Composable
fun <T : Reorderable> BoxScope.ReorderModalSingleType(
    visible: Boolean,
    id: UUID = UUID.randomUUID(),
    TitleContent: @Composable ColumnScope.() -> Unit = { DefaultReorderTitle() },
    initialItems: List<T>,
    dismiss: () -> Unit,
    onUpdateItemOrderNum: (item: T, newOrderNum: Double) -> Unit = { _, _ -> },
    onReordered: ((List<T>) -> Unit)? = null,
    ItemContent: @Composable RowScope.(Int, T) -> Unit
) {
    ReorderModal<T>(
        visible = visible,
        id = id,
        initialItems = initialItems,
        TitleContent = TitleContent,
        dismiss = dismiss,
        onUpdateItemOrderNum = { _, item, newOrderNum ->
            onUpdateItemOrderNum(item, newOrderNum)
        },
        onReordered = { listAny ->
            onReordered?.invoke(
                listAny as? List<T> ?: error("List<T> cast exception.")
            )
        },
        ItemContent = { index, itemAny ->
            ItemContent(index, itemAny as T)
        }
    )
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming", "FunctionParameterNaming")
@Composable
fun <T : Reorderable> BoxScope.ReorderModal(
    visible: Boolean,
    id: UUID = UUID.randomUUID(),
    TitleContent: @Composable ColumnScope.() -> Unit = { DefaultReorderTitle() },
    initialItems: List<Any>,
    dismiss: () -> Unit,
    onUpdateItemOrderNum: (
        itemsInNewOrder: List<Any>,
        item: T,
        newOrderNum: Double
    ) -> Unit = { _, _, _ -> },
    onReordered: ((List<Any>) -> Unit)? = null,
    ItemContent: @Composable RowScope.(Int, Any) -> Unit
) {
    var items by remember(id, initialItems) { mutableStateOf(initialItems) }
    var reOrderedList: List<Any>? by remember {
        mutableStateOf(null)
    }
    var orderNumUpdates by remember {
        mutableStateOf(
            mapOf<T, Double>()
        )
    }

    IvyModal(
        id = id,
        visible = visible,
        scrollState = null,
        dismiss = dismiss,
        PrimaryAction = {
            ModalSave(
                modifier = Modifier.testTag("reorder_done"),
            ) {
                orderNumUpdates.forEach { (item, newOrderNum) ->
                    onUpdateItemOrderNum(items, item, newOrderNum)
                }

                onReordered?.invoke(reOrderedList ?: items)
                dismiss()
            }
        }
    ) {
        Spacer(Modifier.height(16.dp))

        TitleContent()

        Spacer(Modifier.height(16.dp))

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = {
                RecyclerView(it).apply {
                    val itemTouchHelper = itemTouchHelper<T>()
                    adapter = Adapter<T>(
                        itemTouchHelper = itemTouchHelper,
                        ItemContent = ItemContent,
                        addItemOrderNumUpdate = { item, newOrderNum ->
                            orderNumUpdates = orderNumUpdates
                                .toMutableMap()
                                .apply {
                                    this[item] = newOrderNum
                                }
                        },
                        onReorderInternalList = { reorderedItems ->
                            items = reorderedItems
                            reOrderedList = reorderedItems
                        }
                    )
                    layoutManager = LinearLayoutManager(it)
                    itemTouchHelper.attachToRecyclerView(this)

                    adapter<T>().display(items)
                }
            },
            update = {
            }
        )
    }
}

/**
 * One reorderable row: a Material card holding the caller's content with a drag handle at the
 * end. While dragged it lifts (shadow) and switches to the secondary container colour.
 */
@Composable
private fun ReorderRowCard(
    dragging: Boolean,
    position: Int,
    onStartDrag: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val elevation by animateDpAsState(
        targetValue = if (dragging) DraggingElevation else 0.dp,
        label = "reorderElevation",
    )
    val container by animateColorAsState(
        targetValue = if (dragging) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "reorderContainer",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RowOuterHorizontal, vertical = RowOuterVertical),
        shape = MaterialTheme.shapes.large,
        color = container,
        shadowElevation = elevation,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = RowMinHeight)
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { onStartDrag() })
                    }
                    .testTag("reorder_drag_handle"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = "reorder_$position",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("UNCHECKED_CAST")
private class Adapter<T : Reorderable>(
    private val itemTouchHelper: ItemTouchHelper,
    private val ItemContent: @Composable RowScope.(Int, Any) -> Unit,
    private val addItemOrderNumUpdate: (item: T, orderNum: Double) -> Unit,
    private val onReorderInternalList: (List<Any>) -> Unit
) : RecyclerView.Adapter<Adapter<T>.ItemViewHolder>() {
    val data = mutableListOf<Any>()

    @SuppressLint("NotifyDataSetChanged")
    fun display(items: List<Any>) {
        data.clear()
        data.addAll(items)
        notifyDataSetChanged()
    }

    fun moveItem(from: Int, to: Int) {
        data.swap(from, to)
        notifyItemMoved(from, to)
    }

    fun onItemMoved(item: T, to: Int) {
        val newOrderNum = calculateOrderNum<T>(
            itemsInNewOrder = data,
            to = to
        )

        data[to] = item.withNewOrderNum(newOrderNum) as? T
            ?: error("Incorrect Reorderable implementation for $item")
        addItemOrderNumUpdate(item, newOrderNum)
    }

    fun onReorderInternalList() {
        onReorderInternalList(data)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.display(
            item = data[position],
            ItemContent = ItemContent,
            position = position
        )
    }

    override fun getItemCount() = data.size

    inner class ItemViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {

        /** True while ItemTouchHelper is dragging this row. */
        var dragging by mutableStateOf(false)

        fun display(
            item: Any,
            ItemContent: @Composable RowScope.(Int, Any) -> Unit,
            position: Int
        ) {
            (itemView as ComposeView).setContent {
                if (item as? T != null) {
                    ReorderRowCard(
                        dragging = dragging,
                        position = position,
                        onStartDrag = { itemTouchHelper.startDrag(this@ItemViewHolder) },
                    ) {
                        ItemContent(adapterPosition, item)
                    }
                } else {
                    // Non-reorderable rows (section headers) render as plain content.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TitleInset, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ItemContent(adapterPosition, item)
                    }
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Reorderable> itemTouchHelper(): ItemTouchHelper {
    // 1. Note that I am specifying all 4 directions.
    //    Specifying START and END also allows
    //    more organic dragging than just specifying UP and DOWN.
    val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(UP or DOWN, 0) {
        var movedItem: T? = null
        var finalTo: Int? = null

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val adapter = recyclerView.adapter<T>()

            val from = viewHolder.adapterPosition
            val to = target.adapterPosition

            val targetItem = adapter.data[from] as? T ?: return false

            if (movedItem == null) {
                movedItem = targetItem
            }
            finalTo = to

            adapter.moveItem(from, to)

            return true
        }

        override fun onSwiped(
            viewHolder: RecyclerView.ViewHolder,
            direction: Int
        ) {
            // 4. Code block for horizontal swipe.
            //    ItemTouchHelper handles horizontal swipe as well, but
            //    it is not relevant with reordering. Ignoring here.
        }

        // 1. This callback is called when a ViewHolder is selected.
        //    We highlight the ViewHolder here.
        override fun onSelectedChanged(
            viewHolder: RecyclerView.ViewHolder?,
            actionState: Int
        ) {
            super.onSelectedChanged(viewHolder, actionState)

            if (actionState == ACTION_STATE_DRAG) {
                (viewHolder as? Adapter<*>.ItemViewHolder)?.dragging = true
            }
        }

        override fun clearView(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ) {
            super.clearView(recyclerView, viewHolder)
            (viewHolder as? Adapter<*>.ItemViewHolder)?.dragging = false
            val adapter = recyclerView.adapter<T>()
            if (movedItem != null && finalTo != null) {
                adapter.onItemMoved(movedItem!!, finalTo!!)
            }
            adapter.onReorderInternalList()

            movedItem = null
            finalTo = null
        }
    }
    return ItemTouchHelper(simpleItemTouchCallback)
}

@Suppress("UNCHECKED_CAST")
private fun <T : Reorderable> RecyclerView.adapter() = adapter as? Adapter<T>
    ?: error("Adapter not set or wrong adapter set to recyclerview.")

@Composable
fun ReorderButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    CircleButtonFilled(
        modifier = modifier
            .testTag("reorder_button"),
        icon = R.drawable.ic_reorder,
        onClick = onClick
    )
}

@Suppress("UNCHECKED_CAST")
private fun <T : Reorderable> calculateOrderNum(
    itemsInNewOrder: List<*>,
    to: Int
): Double {
    val itemBefore = itemsInNewOrder.getOrNull(to - 1) as? T
    val itemAfter = itemsInNewOrder.getOrNull(to + 1) as? T

    return when {
        itemBefore != null && itemAfter != null -> {
            numberBetween(
                itemBefore.getItemOrderNum(),
                itemAfter.getItemOrderNum()
            )
        }

        itemBefore != null && itemAfter == null -> {
            // It's last in it's priority
            itemBefore.getItemOrderNum() + 1
        }

        itemBefore == null && itemAfter != null -> {
            // It's first in it's priority
            itemAfter.getItemOrderNum() - 1
        }

        else -> 0.0
    }
}
