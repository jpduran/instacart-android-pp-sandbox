package com.instacart.android.challenges

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.instacart.android.challenges.network.DeliveryItem
import com.instacart.android.challenges.network.NetworkService
import com.instacart.android.challenges.network.OrdersResponse
import kotlinx.coroutines.launch
import okhttp3.internal.wait
import java.io.IOException

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    interface UpdateListener {
        fun onUpdate(state: ItemListViewState)
    }

    private var itemListViewState: ItemListViewState = ItemListViewState(" ", emptyList())
    private var listener: UpdateListener? = null

    init {
//        val items = listOf(
//            ItemRow("Cabbage"),
//            ItemRow("Apple"),
//            ItemRow("Bread")
//        )

        //itemListViewState = ItemListViewState("Delivery Items", items)

        getItemsFromNetwork()
    }

    private fun getItemsFromNetwork() {
        viewModelScope.launch {
            try {
                val results: MutableList<DeliveryItem> = ArrayList()
                val ordersIds = NetworkService.api.fetchOrdersCoroutine()
                for(id in ordersIds.orders) {
                    val orderResponse = NetworkService.api.fetchOrderByIdCoroutine(id)
                    results.addAll(orderResponse.items)
                }

                var itemsRow : MutableList<ItemRow> = results.map {
                    ItemRow(it.name, it.count)
                }.toMutableList()

                val itemsGrouped = itemsRow
                    .groupingBy { it.name }
                    .eachCount().filter { it.value > 1 }

                for (repeatedInstance in itemsGrouped) {
                    val totalCount = itemsRow.filter { it.name == repeatedInstance.key }
                                        .map { it.count }.sum()
                    itemsRow = itemsRow.filter { it.name != repeatedInstance.key }.toMutableList()
                    itemsRow.add(ItemRow(repeatedInstance.key, totalCount))
                }

                itemListViewState = ItemListViewState("Delivery Items", itemsRow)
                listener?.onUpdate(itemListViewState)
            } catch (e: IOException){
                // :(
            }
        }
    }

    fun setStateUpdateListener(listener: UpdateListener?) {
        this.listener = listener

        listener?.onUpdate(itemListViewState)
    }
}
