package com.maxrave.kotlinytmusicscraper.models.response

import com.maxrave.kotlinytmusicscraper.models.AccountInfo
import com.maxrave.kotlinytmusicscraper.models.Runs
import com.maxrave.kotlinytmusicscraper.models.Thumbnails
import kotlinx.serialization.Serializable

@Serializable
data class AccountMenuResponse(
    val actions: List<Action>,
) {
    @Serializable
    data class Action(
        val openPopupAction: OpenPopupAction,
    ) {
        @Serializable
        data class OpenPopupAction(
            val popup: Popup,
        ) {
            @Serializable
            data class Popup(
                val multiPageMenuRenderer: MultiPageMenuRenderer,
            ) {
                @Serializable
                data class MultiPageMenuRenderer(
                    val header: Header?,
                ) {
                    @Serializable
                    data class Header(
                        val activeAccountHeaderRenderer: ActiveAccountHeaderRenderer,
                    ) {
                        @Serializable
                        data class ActiveAccountHeaderRenderer(
                            val accountName: Runs?,
                            val accountPhoto: Thumbnails?,
                            val channelHandle: Runs?,
                        ) {
                            fun toAccountInfo(): AccountInfo? {
                                val accountNameText = accountName?.runs?.firstOrNull()?.text
                                val channelHandleText = channelHandle?.runs?.firstOrNull()?.text ?: " "
                                val accountPhotoThumbnails = accountPhoto?.thumbnails

                                return if (accountNameText != null && channelHandleText != null && accountPhotoThumbnails != null) {
                                    AccountInfo(
                                        accountNameText,
                                        channelHandleText,
                                        accountPhotoThumbnails
                                    )
                                } else {
                                    null
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
