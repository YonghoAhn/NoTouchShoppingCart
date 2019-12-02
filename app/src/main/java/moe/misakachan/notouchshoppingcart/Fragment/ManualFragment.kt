package moe.misakachan.notouchshoppingcart.Fragment


import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.fragment_manual.*

import moe.misakachan.notouchshoppingcart.R

/**
 * A simple [Fragment] subclass.
 */
class ManualFragment : Fragment() , View.OnClickListener{
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        switch1.setOnCheckedChangeListener { _, isChecked ->
            val localBroadcastManager = LocalBroadcastManager.getInstance(context!!)
            val msgIntent = Intent("ACTION_CONTROL_MODE_CHANGE")
            msgIntent.putExtra("mode", isChecked)
            localBroadcastManager.sendBroadcast(msgIntent)
        }

        imgUp.setOnClickListener(this)
        imgDown.setOnClickListener(this)
        imgLeft.setOnClickListener(this)
        imgRight.setOnClickListener(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_manual, container, false)
    }

    override fun onClick(v: View?) {
        if(switch1.isChecked) {
            val localBroadcastManager = LocalBroadcastManager.getInstance(context!!)
            val msgIntent = Intent("ACTION_MANUAL_CONTROL")
            var direction = ""
            when (v?.id) {
                R.id.imgUp -> direction = "U"
                R.id.imgDown -> direction = "D"
                R.id.imgLeft -> direction = "L"
                R.id.imgRight -> direction = "R"
            }
            msgIntent.putExtra("direction", direction)
            localBroadcastManager.sendBroadcast(msgIntent)
        }
    }


}
