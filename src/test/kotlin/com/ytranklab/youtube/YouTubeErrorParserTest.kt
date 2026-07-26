package com.ytranklab.youtube

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YouTubeErrorParserTest {
    private val parser = YouTubeErrorParser()

    @Test
    fun `not found errors are not retryable`() {
        val error = parser.parse(
            """
            {
              "error": {
                "code": 404,
                "message": "The video that you are trying to retrieve cannot be found.",
                "errors": [
                  { "reason": "notFound" }
                ],
                "status": "NOT_FOUND"
              }
            }
            """.trimIndent(),
        )

        assertFalse(error.retryable)
    }

    @Test
    fun `transient errors remain retryable`() {
        val error = parser.parse(
            """
            {
              "error": {
                "code": 503,
                "message": "Backend Error",
                "errors": [
                  { "reason": "backendError" }
                ],
                "status": "UNAVAILABLE"
              }
            }
            """.trimIndent(),
        )

        assertTrue(error.retryable)
    }
}
