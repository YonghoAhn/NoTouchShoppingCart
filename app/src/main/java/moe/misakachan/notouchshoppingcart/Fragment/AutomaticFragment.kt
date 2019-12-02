package moe.misakachan.notouchshoppingcart.Fragment


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.fragment_automatic.*

import moe.misakachan.notouchshoppingcart.R

/**
 * A simple [Fragment] subclass.
 */
class AutomaticFragment : Fragment() {

    /**
     * This receiver will receive msg from Cart.
     * Handle message: OK/Error/Another
     */
    private val bluetoothMessageReceiver : BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //
        }
    }

    /**
     * This receiver will receive step counter msg from Service.
     */
    private val stepMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val stepCount = intent?.getIntExtra("step",0)
            txtStep.text = stepCount.toString()
        }
    }

    private val azimuthReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val azimuth = intent?.getFloatExtra("azimuth",0f)
            txtAzimuth.text = azimuth.toString()
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(context!!).registerReceiver(stepMessageReceiver,IntentFilter("ACTION_STEP_DETECTED"))
        LocalBroadcastManager.getInstance(context!!).registerReceiver(bluetoothMessageReceiver, IntentFilter("ACTION_BT_SERIAL_RECEIVE"))
        LocalBroadcastManager.getInstance(context!!).registerReceiver(azimuthReceiver, IntentFilter("ACTION_AZIMUTH_CHANGED"))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(stepMessageReceiver)
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(bluetoothMessageReceiver)
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(azimuthReceiver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_automatic, container, false)
    }


}
