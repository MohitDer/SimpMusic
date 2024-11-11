package com.google.rvadapter


import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.ads.nativead.AppNativeAdView
import com.maxrave.simpmusic.ads.nativead.NativeAdStyle
import java.util.*

class AdmobNativeAdAdapter private constructor(private val mParam: Param) :
    RecyclerViewAdapterWrapper(
        mParam.adapter as RecyclerView.Adapter<RecyclerView.ViewHolder>
    ) {
    init {
        assertConfig()
        setSpanAds()
    }

    private fun assertConfig() {
        mParam.gridLayoutManager?.let { layoutManager ->
            val nCol = layoutManager.spanCount
            require(mParam.adItemInterval % nCol == 0) {
                String.format(
                    "The adItemInterval (%d) is not divisible by number of columns in GridLayoutManager (%d)",
                    mParam.adItemInterval,
                    nCol
                )
            }
        }
    }

    private fun convertAdPosition2OrgPosition(position: Int): Int {
        return position - (position + 1) / (mParam.adItemInterval + 1)
    }

    override fun getItemCount(): Int {
        val realCount = super.getItemCount()
        return realCount + realCount / mParam.adItemInterval
    }

    override fun getItemViewType(position: Int): Int {
        return if (isAdPosition(position)) {
            TYPE_FB_NATIVE_ADS
        } else super.getItemViewType(convertAdPosition2OrgPosition(position))
    }

    private fun isAdPosition(position: Int): Boolean {
        return (position + 1) % (mParam.adItemInterval + 1) == 0
    }

    private fun onBindAdViewHolder(holder: RecyclerView.ViewHolder) {
        val adHolder = holder as AdViewHolder
        if (mParam.forceReloadAdOnBind || !adHolder.loaded) {
            val adLoader = AdLoader.Builder(adHolder.itemView.context, mParam.admobNativeId!!)
                .forNativeAd { nativeAd ->
                    Log.e("admobnative", "loaded")
                    val builder = NativeAdStyle.Builder()
                    builder.withPrimaryTextSize(11f)
                    builder.withTertiaryTextSize(06f)
                    builder.withCallToActionTextSize(11f)
                    when (mParam.layout) {
                        0 -> {
                            adHolder.templatesmall.visibility = View.VISIBLE
                            adHolder.templatesmall.setStyles(builder.build())
                            adHolder.templatesmall.setNativeAd(nativeAd)
                        }
                        1 -> {
                            adHolder.templatemedium.visibility = View.VISIBLE
                            adHolder.templatemedium.setStyles(builder.build())
                            adHolder.templatemedium.setNativeAd(nativeAd)
                        }
                    }
                    adHolder.loaded = true
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        super.onAdFailedToLoad(loadAdError)
                        Log.e("admobnative", "error:$loadAdError")
                        adHolder.adContainer.visibility = View.GONE
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .build()
                )
                .build()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_FB_NATIVE_ADS) {
            onBindAdViewHolder(holder)
        } else {
            super.onBindViewHolder(holder, convertAdPosition2OrgPosition(position))
        }
    }

    private fun onCreateAdViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val adLayoutOutline = inflater.inflate(mParam.itemContainerLayoutRes, parent, false)
        val vg = adLayoutOutline.findViewById<ViewGroup>(mParam.itemContainerId)
        val adLayoutContent = inflater.inflate(R.layout.item_admob_native_ad, parent, false) as LinearLayout
        vg.addView(adLayoutContent)
        return AdViewHolder(adLayoutOutline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_FB_NATIVE_ADS) {
            onCreateAdViewHolder(parent)
        } else super.onCreateViewHolder(parent, viewType)
    }

    private fun setSpanAds() {
        mParam.gridLayoutManager?.let { layoutManager ->
            val spl = layoutManager.spanSizeLookup
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (isAdPosition(position)) layoutManager.spanCount else 1
                }
            }
        }
    }

    private class Param {
        var admobNativeId: String? = null
        var adapter: RecyclerView.Adapter<*>? = null
        var adItemInterval = 0
        var forceReloadAdOnBind = false
        var layout = 0

        @LayoutRes
        var itemContainerLayoutRes = 0

        @IdRes
        var itemContainerId = 0
        var gridLayoutManager: GridLayoutManager? = null
    }

    class Builder private constructor(private val mParam: Param) {
        fun adItemInterval(interval: Int): Builder {
            mParam.adItemInterval = interval
            return this
        }

        fun adLayout(@LayoutRes layoutContainerRes: Int, @IdRes itemContainerId: Int): Builder {
            mParam.itemContainerLayoutRes = layoutContainerRes
            mParam.itemContainerId = itemContainerId
            return this
        }

        fun build(): AdmobNativeAdAdapter {
            return AdmobNativeAdAdapter(mParam)
        }

        fun enableSpanRow(layoutManager: GridLayoutManager?): Builder {
            mParam.gridLayoutManager = layoutManager
            return this
        }

        fun adItemIterval(i: Int): Builder {
            mParam.adItemInterval = i
            return this
        }

        fun forceReloadAdOnBind(forced: Boolean): Builder {
            mParam.forceReloadAdOnBind = forced
            return this
        }

        companion object {
            fun with(
                placementId: String?,
                wrapped: RecyclerView.Adapter<*>?,
                layout: String
            ): Builder {
                val param = Param()
                param.admobNativeId = placementId
                param.adapter = wrapped
                when (layout.lowercase(Locale.getDefault())) {
                    "small" -> param.layout = 0
                    "medium" -> param.layout = 1
                    else -> param.layout = 2
                }
                param.adItemInterval = DEFAULT_AD_ITEM_INTERVAL
                param.itemContainerLayoutRes = R.layout.item_admob_native_ad_outline
                param.itemContainerId = R.id.ad_container
                param.forceReloadAdOnBind = true
                return Builder(param)
            }
        }
    }

    private class AdViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
        var templatesmall: AppNativeAdView = view.findViewById(R.id.admob_template_small)
        var templatemedium: AppNativeAdView = view.findViewById(R.id.admob_template_medium)
        var adContainer: LinearLayout = view.findViewById(R.id.ad_container)
        var loaded: Boolean = false
    }

    companion object {
        private const val TYPE_FB_NATIVE_ADS = 900
        private const val DEFAULT_AD_ITEM_INTERVAL = 10
    }
}