package moe.misakachan.notouchshoppingcart.Fragment


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.fragment_manual.*

import moe.misakachan.notouchshoppingcart.R
import moe.misakachan.notouchshoppingcart.Service.BluetoothDataService

/**
 * A simple [Fragment] subclass.
 */
class ManualFragment : Fragment(){

    private lateinit var mService: BluetoothDataService
    private var mBound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as BluetoothDataService.BluetoothDataServiceBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val localBroadcastManager = LocalBroadcastManager.getInstance(context!!)
        Intent(context!!, BluetoothDataService::class.java).also { intent ->
            activity!!.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        switch1.setOnCheckedChangeListener { _, isChecked ->
            mService.isAutoMode = isChecked
        }
        imgUp.setOnClickListener{
            mService.sendManualControl("U")
        }
        imgDown.setOnClickListener{
            mService.sendManualControl("D")
        }
        imgLeft.setOnClickListener{
            mService.sendManualControl("L")
        }
        imgRight.setOnClickListener{
            mService.sendManualControl("R")
        }
        btnStop.setOnClickListener{
            mService.sendManualControl("S")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_manual, container, false)
    }

    override fun onStop() {
        super.onStop()
        activity!!.unbindService(connection)
        mBound = false
    }
}
