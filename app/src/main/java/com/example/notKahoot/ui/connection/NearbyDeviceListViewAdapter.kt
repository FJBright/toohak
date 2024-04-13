package com.example.notKahoot.ui.connection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.notKahoot.R
import com.example.notKahoot.ui.viewModel.NearbyConnectionWrapper

class NearbyDeviceListViewAdapter(
    val devices: MutableList<NearbyConnectionWrapper.Endpoint>,
    val onClick: ((NearbyConnectionWrapper.Endpoint) -> Unit)?
):
    RecyclerView.Adapter<NearbyDeviceListViewAdapter.ListItemViewHolder>()
{
    class ListItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val nameView: TextView
        val addressView: TextView
        var onClick: ((View?) -> Unit)? = null

        init {
            nameView = itemView.findViewById(R.id.device_name)
            addressView = itemView.findViewById(R.id.device_address)
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            onClick?.invoke(view)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.nearby_device_list_item, parent, false)
        return ListItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        val device = devices[position]
        holder.nameView.text = device.name
        holder.addressView.text = device.id
        holder.onClick = { onClick?.invoke(device) }
    }

    override fun getItemCount(): Int {
        return devices.size
    }
}