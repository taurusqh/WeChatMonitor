package com.wechatmonitor.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * GLM API 服务接口
 */
interface GLMApiService {
    @POST("chat/completions")
    suspend fun analyzeMessage(
        @Header("Authorization") authorization: String,
        @Body request: GLMRequest
    ): GLMResponse

    @POST("chat/completions")
    suspend fun generateSummary(
        @Header("Authorization") authorization: String,
        @Body request: GLMRequest
    ): GLMResponse
}

/**
 * GLM 请求
 */
@Serializable
data class GLMRequest(
    val model: String,
    val messages: List<GLMMessage>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 1024
)

/**
 * GLM 消息
 */
@Serializable
data class GLMMessage(
    val role: String,  // system, user, assistant
    val content: String
)

/**
 * GLM 响应
 */
@Serializable
data class GLMResponse(
    val id: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<GLMChoice>,
    val usage: GLMUsage? = null
)

/**
 * GLM 选择
 */
@Serializable
data class GLMChoice(
    val index: Int,
    val message: GLMMessage,
    val finish_reason: String? = null
)

/**
 * GLM 使用情况
 */
@Serializable
data class GLMUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

/**
 * GLM 分析结果
 */
@Serializable
data class GLMAnalysisResult(
    val isImportant: Boolean,
    val score: Float,
    val reason: String
)
