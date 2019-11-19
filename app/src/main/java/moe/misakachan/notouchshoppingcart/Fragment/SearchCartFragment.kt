package moe.misakachan.notouchshoppingcart.Fragment


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_search_cart.*

import moe.misakachan.notouchshoppingcart.R

/**
 * A simple [Fragment] subclass.
 */
class SearchCartFragment : Fragment() {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        btnFindCart.setOnClickListener {
            findNavController().navigate(R.id.action_searchCartFragment_to_cartListFragment)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search_cart, container, false)
    }

}
