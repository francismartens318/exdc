/*
 * Copyright (c) 2024 Exalate (https://exalate.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package customconnectornode.discourse.http

import com.exalate.domain.BasicCredentials
import com.exalate.domain.TrackerCredentials
import com.exalate.domain.http.GroovyHttpRequest
import customconnectornode.services.api.IHttpRequestAuthenticator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class CustomConnectorHttpAuthenticator implements IHttpRequestAuthenticator {

    protected static Logger LOG = LoggerFactory.getLogger(CustomConnectorHttpAuthenticator.class)

    // Implement this method returning the proper headers according to the connector
    abstract Map<String, List<String>> getAuthorizationMap(GroovyHttpRequest groovyHttpRequest, BasicCredentials credentials)

    @Override
    GroovyHttpRequest authenticateRequest(GroovyHttpRequest groovyHttpRequest, Optional<TrackerCredentials> credentials) {
        LOG.debug("Authenticating Request ${groovyHttpRequest.toString()}")
        Map<String, List<String>> headers = getModifiedHeaders(groovyHttpRequest, credentials)
        return createGroovyHttpRequestWithHeaders(groovyHttpRequest, headers)
    }

    // Changed from static to instance method
    private Map<String, List<String>> getModifiedHeaders(GroovyHttpRequest groovyHttpRequest, Optional<TrackerCredentials> credentials) {
        return credentials
                .map { TrackerCredentials tc ->
                    if (tc instanceof BasicCredentials) {
                        Map<String, List<String>> headers = [:]
                        if (groovyHttpRequest.headers) {
                            headers.putAll(groovyHttpRequest.headers)
                        }
                        headers.putAll(getAuthorizationMap(groovyHttpRequest, (BasicCredentials) tc))
                        headers
                    } else {
                        LOG.error("Invalid credentials type: ${tc.getClass().getName()}")
                        throw new RuntimeException("Unsupported credentials type: ${tc.getClass().getName()}")
                    }

                }.orElseThrow { new RuntimeException("Http Credentials missing!") }
    }

    // This can remain static since it doesn't depend on instance state
    private static GroovyHttpRequest createGroovyHttpRequestWithHeaders(
            GroovyHttpRequest groovyHttpRequest,
            Map<String, List<String>> headers
    ) {
        return new GroovyHttpRequest(
                groovyHttpRequest.method,
                groovyHttpRequest.url,
                groovyHttpRequest.body,
                groovyHttpRequest.queryParameters,
                headers
        )
    }
}