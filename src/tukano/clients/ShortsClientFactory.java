package tukano.clients;

import java.net.URI;

import tukano.Discovery;
import tukano.api.java.Shorts;
import tukano.clients.rest.RestShortsClient;

public class ShortsClientFactory{
    // Discovery mechanism to fetch the URI dynamically
    private static final URI discoverShortsServiceURI() {
        Discovery discovery = Discovery.getInstance();
        return discovery.knownUrisOf("shorts", 1).get(0);
    }

    public static Shorts getClient() {
        URI serverURI = discoverShortsServiceURI(); // Discover the URI dynamically
        if (serverURI != null) {
            if (serverURI.getPath().endsWith("rest")) {
                return new RestShortsClient(serverURI);
            } else {
                return null; 
            }
        } else {
            return null;
        }
    }
}