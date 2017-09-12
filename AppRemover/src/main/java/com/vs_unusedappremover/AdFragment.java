package com.vs_unusedappremover;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.vs_unusedappremover.common.GA;

public class AdFragment extends Fragment {

    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.admob_banner, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        AdView mAdView = (AdView) getView().findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.setAdListener(adListener);
        mAdView.loadAd(adRequest);
    }

    private final AdListener adListener = new AdListener() {

        @Override
        public void onAdFailedToLoad(int errorCode) {
            GA.event("Ad", "failed to receive ad" + errorCode);
        }

        @Override
        public void onAdLoaded() {
            GA.event("Ad", "received ad");
            if (view != null) {
                view.setVisibility(View.VISIBLE);
            }
        }
    };
}
