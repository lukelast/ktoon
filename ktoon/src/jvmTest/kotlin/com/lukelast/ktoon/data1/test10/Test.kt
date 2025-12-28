package com.lukelast.ktoon.data1.test10

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/** https://github.com/toon-format/spec/blob/main/examples/conversions/api-response.toon */
class Test10 : Runner() {
    override fun run() = doTest(data)
}

val data =
    UserResponse(
        data =
            UserData(
                id = 42,
                type = "user",
                attributes =
                    UserAttributes(name = "Ada Lovelace", email = "ada@example.com", active = true),
            ),
        meta =
            MetaData(
                request = RequestMeta(id = "req_abc123", timestamp = "2025-01-15T10:30:00Z"),
                response = ResponseMeta(duration = 42, cached = false),
            ),
    )

@Serializable data class UserResponse(val data: UserData, val meta: MetaData)

@Serializable data class UserData(val id: Int, val type: String, val attributes: UserAttributes)

@Serializable data class UserAttributes(val name: String, val email: String, val active: Boolean)

@Serializable data class MetaData(val request: RequestMeta, val response: ResponseMeta)

@Serializable
data class RequestMeta(
    val id: String,
    val timestamp: String, // You can also use Instant with a KSerializer if needed
)

@Serializable data class ResponseMeta(val duration: Int, val cached: Boolean)
