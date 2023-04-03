package moluccus.app.ai

import com.google.errorprone.annotations.Keep
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@Keep
interface OpenAiApi {
    @POST("completions")
    suspend fun getResponse(@Header("Authorization") apiKey: String, @Body request: ChatRequest): ChatResponse
}