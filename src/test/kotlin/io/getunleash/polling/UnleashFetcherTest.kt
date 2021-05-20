package io.getunleash.polling

import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UnleashFetcherTest {
    val complicatedVariants = """{
            	"toggles": [
                    {
                        "name": "variantToggle",
                        "enabled": true,
                        "variant": {
                            "name": "green",
                            "payload": {
                                "type": "number",
                                "value": 54
                            }
                        }
                    }, {
                        "name": "featureToggle",
                        "enabled": true,
                        "variant": {
                            "name": "disabled"
                        }
                    }, {
                        "name": "simpleToggle",
                        "enabled": true
                        "variant": {
                            "name": "red",
                            "payload": {
                                "type": "json",
                                "value": { "key": "value" }
                            }
                        }
                    }
                ]
            }""".trimIndent()
    @Test
    fun `Fetcher uses ETags if present`() {
        val server = MockWebServer()
        val fetcher = UnleashFetcher(UnleashConfig(proxyUrl = server.url("/proxy").toString(), clientSecret = "my-secret"))
        val fakeEtagHeader = "etag-1"
        server.enqueue(MockResponse().setResponseCode(200).setBody(complicatedVariants).setHeader("ETag", fakeEtagHeader))
        server.enqueue(MockResponse().setResponseCode(304))

        val response = fetcher.getResponseAsync(UnleashContext.newBuilder().userId("hello").build()).get()
        assertThat(response.isFetched()).isTrue
        assertThat(response.isFailed()).isFalse
        assertThat(response.isNotModified()).isFalse

        val cachedResponse = fetcher.getResponseAsync(UnleashContext.newBuilder().userId("hello").build()).get()
        assertThat(cachedResponse.isNotModified()).isTrue
        assertThat(cachedResponse.isFetched()).isFalse
        assertThat(cachedResponse.isFailed()).isFalse

        assertThat(server.takeRequest().getHeader("If-None-Match")).isNull()
        assertThat(server.takeRequest().getHeader("If-None-Match")).isEqualTo(fakeEtagHeader)
    }
}