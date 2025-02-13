package customconnectornode.discourse.http

import com.exalate.domain.BasicCredentials
import com.exalate.domain.TrackerCredentials
import com.exalate.domain.http.GroovyHttpRequest
import customconnectornode.services.api.IHttpRequestAuthenticator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HttpRequestAuthenticatorImpl implements IHttpRequestAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestAuthenticatorImpl.class);

    @Override
    GroovyHttpRequest authenticateRequest(GroovyHttpRequest groovyHttpRequest, Optional<TrackerCredentials> credentials) {
        return credentials
                .map { TrackerCredentials tc ->
                    if (tc instanceof BasicCredentials) {
                        log.info("Authenticating request with BasicCredentials!");

                        try {
                            //  More efficient header modification (assuming GroovyHttpRequest allows it)
                            GroovyHttpRequest authenticatedRequest = (GroovyHttpRequest) groovyHttpRequest.clone() // Clone the request
                            authenticatedRequest.headers.put('Api-Username', [tc.user()])
                            authenticatedRequest.headers.put('Api-Key', [tc.password()])

                            return authenticatedRequest;

                        } catch (NullPointerException e) {
                            log.error("Headers are null: ${e.getMessage()}", e)
                            throw new RuntimeException("Invalid request: Headers are missing")
                        }


                    } else {
                        log.error("Invalid credentials type: ${tc.getClass().getName()}");
                        throw new RuntimeException("Unsupported credentials type: ${tc.getClass().getName()}")
                        // Throw exception
                    }
                }.orElseThrow { new RuntimeException("Http Credentials missing!") } // Use custom exception
    }
}