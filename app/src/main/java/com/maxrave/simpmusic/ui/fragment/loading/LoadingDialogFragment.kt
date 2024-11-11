package com.maxrave.simpmusic.ui.fragment.loading

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.utils.LanguagePreference

class LoadingDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_loading_dialog, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent)
//        LanguagePreference.setAppLanguage(requireContext(), LanguagePreference.getSelectedLanguage(requireContext()))

    }
}
