package com.vs_unusedappremover

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.vs_unusedappremover.common.GA

class AdFragment : Fragment() {

    private var view: View? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        view = inflater!!.inflate(R.layout.admob_banner, container, false)
        return view
    }

    override fun onActivityCreated(bundle: Bundle?) {
        super.onActivityCreated(bundle)
        val mAdView = getView()!!.findViewById(R.id.adView) as AdView
        val adRequest = AdRequest.Builder().build()
        mAdView.adListener = adListener
        mAdView.loadAd(adRequest)
    }

    private val adListener = object : AdListener() {

        override fun onAdFailedToLoad(errorCode: Int) {
            GA.event("Ad", "failed to receive ad" + errorCode)
        }

        override fun onAdLoaded() {
            GA.event("Ad", "received ad")
            if (view != null) {
                view!!.visibility = View.VISIBLE
            }
        }
    }
}
