package moe.misakachan.notouchshoppingcart.Fragment


import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_cart_connection.*
import moe.misakachan.notouchshoppingcart.InitActivity

import moe.misakachan.notouchshoppingcart.R
import moe.misakachan.notouchshoppingcart.Service.BluetoothDataService

/**
 * A simple [Fragment] subclass.
 */
class CartConnectionFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnDisconnect.setOnClickListener {
            activity!!.stopService(Intent(context!!, BluetoothDataService::class.java))
            startActivity(Intent(context!!, InitActivity::class.java    ))
            activity!!.finish()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_cart_connection, container, false)
    }


}
