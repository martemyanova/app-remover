package com.vs_unusedappremover

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.vs_unusedappremover.common.GA
import kotlinx.android.synthetic.main.admob_banner.*

class AdFragment : Fragment() {

    @get:JvmName("getView_")
    private var view: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        view = inflater.inflate(R.layout.admob_banner, container, false)
        return view
    }

    override fun onActivityCreated(bundle: Bundle?) {
        super.onActivityCreated(bundle)
        val adRequest = AdRequest.Builder().build()
        adView.adListener = adListener
        adView.loadAd(adRequest)
    }

    private val adListener = object : AdListener() {

        override fun onAdFailedToLoad(errorCode: Int) {
            GA.event("Ad", "failed to receive ad" + errorCode)
        }

        override fun onAdLoaded() {
            GA.event("Ad", "received ad")
            if (view != null) {
                view?.visibility = View.VISIBLE
            }
        }
    }
}
