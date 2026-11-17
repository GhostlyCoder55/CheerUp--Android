package com.example.cheerup.privacy

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

class ConsentManager private constructor() {

    private var consentInformation: ConsentInformation? = null
    private val isGatheringConsent = AtomicBoolean(false)

    var canRequestAds: Boolean by mutableStateOf(true)
        private set

    var isPrivacyOptionsRequired: Boolean by mutableStateOf(false)
        private set

    fun gatherConsent(activity: Activity, onConsentGatheringComplete: (String?) -> Unit) {
        if (isGatheringConsent.getAndSet(true)) {
            onConsentGatheringComplete(null)
            return
        }

        try {
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()

            val info = UserMessagingPlatform.getConsentInformation(activity)
            consentInformation = info

            info.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        isGatheringConsent.set(false)
                        updateConsentFlags(info)
                        onConsentGatheringComplete(formError?.message)
                    }
                },
                { requestConsentError ->
                    isGatheringConsent.set(false)
                    updateConsentFlags(info)
                    onConsentGatheringComplete(requestConsentError.message)
                }
            )
            updateConsentFlags(info)
        } catch (t: Throwable) {
            isGatheringConsent.set(false)
            onConsentGatheringComplete(t.message)
        }
    }

    fun showPrivacyOptionsForm(activity: Activity, onDismissed: (String?) -> Unit = {}) {
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                consentInformation?.let { updateConsentFlags(it) }
                onDismissed(formError?.message)
            }
        } catch (t: Throwable) {
            onDismissed(t.message)
        }
    }

    /**
     * Builds an AdRequest that defaults to non-personalized ads (npa=1) aligned with CheerUp!'s
     * privacy-first philosophy (NSPrivacyTracking = false on iOS) unless full consent was granted.
     */
    fun buildAdRequest(context: Context? = null): AdRequest {
        val builder = AdRequest.Builder()
        val info = consentInformation ?: context?.let {
            try {
                UserMessagingPlatform.getConsentInformation(it.applicationContext)
            } catch (_: Throwable) {
                null
            }
        }

        val requestNonPersonalized = info == null ||
            info.consentStatus != ConsentInformation.ConsentStatus.OBTAINED

        if (requestNonPersonalized) {
            val extras = Bundle().apply {
                putString("npa", "1")
            }
            builder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
        }
        return builder.build()
    }

    private fun updateConsentFlags(info: ConsentInformation) {
        canRequestAds = info.canRequestAds()
        isPrivacyOptionsRequired =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    companion object {
        val shared: ConsentManager by lazy { ConsentManager() }
    }
}
