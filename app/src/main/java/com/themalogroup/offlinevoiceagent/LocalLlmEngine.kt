package com.themalogroup.offlinevoiceagent

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.Closeable

/**
 * On-device inference for MediaPipe-compatible .task language models.
 * Model-specific context settings are inferred from the original downloaded filename.
 */
class LocalLlmEngine(context: Context, modelPath: String, modelName: String = modelPath) : Closeable {
    private val inference: LlmInference

    init {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(ModelRuntimeConfig.maxTokensFor(modelName))
            .build()
        inference = LlmInference.createFromOptions(context.applicationContext, options)
    }

    @Synchronized
    fun generate(prompt: String): String = inference.generateResponse(prompt)

    @Synchronized
    override fun close() {
        inference.close()
    }
}
