package dk.kofoed.proxy.client;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RestPathHelper {

    String uri = null;

    public String getUri() {
        return this.uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

}
