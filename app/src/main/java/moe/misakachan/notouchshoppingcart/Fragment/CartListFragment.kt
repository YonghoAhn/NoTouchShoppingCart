package moe.misakachan.notouchshoppingcart.Fragment


import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.fragment_cart_list.*

import moe.misakachan.notouchshoppingcart.R

/**
 * A simple [Fragment] subclass.
 */
class CartListFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private val mBluetoothAdapter : BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val mArrayAdapter = ArrayAdapter<String>(context!!, android.R.layout.simple_list_item_1)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 1001)
            if(resultCode != RESULT_OK)
                Toast.makeText(context, "블루투스를 활성화해 주세요.", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if(!mBluetoothAdapter.isEnabled)
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1001)

        swipeCartLayout.setOnRefreshListener(this)
        cartListView.adapter = mArrayAdapter

        val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        activity!!.registerReceiver(BluetoothStateReceiver, intentFilter)

        cartListView.setOnItemClickListener { parent, view, position, id ->
            findNavController().navigate(R.id.action_cartListFragment_to_connetCartFragment)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_cart_list, container, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        activity!!.unregisterReceiver(BluetoothStateReceiver)
    }

    override fun onRefresh() {
        if(mBluetoothAdapter.isDiscovering)
            return
        mArrayAdapter.clear()
        mBluetoothAdapter.startDiscovery()
    }

    private var BluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action : String = intent!!.action!!
            if (BluetoothDevice.ACTION_FOUND == action)
            {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                mArrayAdapter.add(device.name)
                mArrayAdapter.notifyDataSetChanged()
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action)
            {
                swipeCartLayout.isRefreshing = false
                Toast.makeText(context, "검색 완료", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
