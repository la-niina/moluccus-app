package moluccus.app.ai

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    val choices: List<ChatChoice>
)

data class ChatChoice(
    val text: String
)

data class ChatRequest(
    @SerializedName("prompt")
    val prompt: String,
    @SerializedName("temperature")
    val temperature: Double,
    @SerializedName("max_tokens")
    val maxTokens: Int,
    @SerializedName("stop")
    val stop: String
)
