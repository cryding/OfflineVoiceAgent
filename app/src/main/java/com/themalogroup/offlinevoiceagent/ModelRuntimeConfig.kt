package com.themalogroup.offlinevoiceagent

import java.util.Locale

/**
 * Model packages may be built with different KV-cache/context capacities.
 * Use the original filename when available; retain a conservative fallback for unknown packages.
 */
object ModelRuntimeConfig {
    fun maxTokensFor(modelName: String): Int {
        val name = modelName.lowercase(Locale.ROOT)
        return when {
            "_seq128_" in name -> 128
            "_ekv4096" in name -> 4096
            "_ekv1280" in name -> 1280
            else -> 512
        }
    }
}
