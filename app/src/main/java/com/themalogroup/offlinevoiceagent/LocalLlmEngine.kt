package com.themalogroup.offlinevoiceagent

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.Closeable

/**
 * On-device inference for compatible MediaPipe .task language models.
 * A random GGUF file or arbitrary model download is not compatible.
 */
class LocalLlmEngine(context: Context, modelPath: String) : Closeable {
    private val inference: LlmInference

    init {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(512)
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
