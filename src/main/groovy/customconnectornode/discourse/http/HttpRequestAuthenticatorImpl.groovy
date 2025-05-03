package customconnectornode.discourse.http


import com.exalate.domain.BasicCredentials
import com.exalate.domain.http.GroovyHttpRequest


class HttpRequestAuthenticatorImpl extends CustomConnectorHttpAuthenticator {

    @Override
    Map<String, List<String>> getAuthorizationMap(GroovyHttpRequest groovyHttpRequest,BasicCredentials credentials) {
        Map<String, List<String>> newHeaders = new HashMap<>()


        newHeaders.put('Api-Username', [credentials.user()])
        newHeaders.put('Api-Key', [credentials.password()])


        return newHeaders
    }
}
