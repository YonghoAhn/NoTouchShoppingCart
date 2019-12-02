package moe.misakachan.notouchshoppingcart.Fragment


import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_connet_cart.*
import moe.misakachan.notouchshoppingcart.MainActivity

import moe.misakachan.notouchshoppingcart.R
import moe.misakachan.notouchshoppingcart.Service.BluetoothDataService

/**
 * A simple [Fragment] subclass.
 */
class ConnetCartFragment : Fragment() {
    private val args : ConnetCartFragmentArgs by navArgs()
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        textView2.text = args.DeviceName
        btnContinue.setOnClickListener {
            val intent = Intent(context, BluetoothDataService::class.java)
            intent.putExtra("MAC", args.DeviceAddress)
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                //activity?.startForegroundService(intent)
           // else
                activity?.startService(intent)

            startActivity(Intent(context, MainActivity::class.java))
            activity?.finish()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_connet_cart, container, false)
    }


}
