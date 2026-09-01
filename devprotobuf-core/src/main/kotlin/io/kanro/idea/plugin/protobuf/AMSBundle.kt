package io.kanro.idea.plugin.protobuf

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey
import java.util.*

/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.common
 * @Author: 唐煇
 * @CreateTime: 2025-09-12-11:30
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
const val AMS_BUNDLE = "messages.AMS"

object AMSBundle : AbstractBundle(AMS_BUNDLE) {
    fun message(
        @PropertyKey(resourceBundle = AMS_BUNDLE) key: String,
        vararg params: Any,
    ): String {
        val message = getMessage(key, *params)
        return String.format("%s", message.substring(0, 1).uppercase(Locale.getDefault()) + message.substring(1))
    }

    fun message(
        @PropertyKey(resourceBundle = AMS_BUNDLE) key: String
    ): String {
        val message = getMessage(key)
        return String.format("%s", message.substring(0, 1).uppercase(Locale.getDefault()) + message.substring(1))
    }

    fun messagePointer(
        @PropertyKey(resourceBundle = AMS_BUNDLE) key: String,
        vararg params: Any,
    ) = getLazyMessage(key, *params)
}