package customconnectornode.node


import customconnectornode.discourse.http.HttpRequestAuthenticatorImpl
import customconnectornode.services.api.IHttpRequestAuthenticator
import customconnectornode.services.node.spi.IHttpRequestAuthenticatorFactory
import play.api.Application

class HttpRequestAuthenticatorFactory implements IHttpRequestAuthenticatorFactory {
    @Override
    IHttpRequestAuthenticator create(Application application) {
        return new HttpRequestAuthenticatorImpl()
    }
}
