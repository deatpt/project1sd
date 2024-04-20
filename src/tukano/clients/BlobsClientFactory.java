package tukano.clients;

import java.net.URI;
import java.util.List;

import tukano.Discovery;
import tukano.api.java.Blobs;
import tukano.clients.rest.RestBlobsClient;


public class BlobsClientFactory{

    private static int currentIndex = 0;
    
    // Discovery mechanism to fetch the URI dynamically
    public static final URI discoverBlobsServiceURI() {
        Discovery discovery = Discovery.getInstance();
        return discovery.knownUrisOf("blobs", 1).get(0); // Assuming "users" is the service name
    }

    public static Blobs getClient() {
        URI serverURI = discoverBlobsServiceURI(); // Discover the URI dynamically
        if (serverURI != null) {
            if (serverURI.getPath().endsWith("rest")) {
                return new RestBlobsClient(serverURI);
            } else {
                return null;
            }
        } else {
            return null;
        }

    }

    public static URI getURI() {
            Discovery discovery = Discovery.getInstance();

            var serverURI = discovery.knownUrisOf("blobs", 1);

            if (serverURI == null || serverURI.isEmpty()) {
                serverURI = discovery.knownUrisOf("blobs", 1);
            }

            // Obtém o URI com base no índice atual
            URI uri = serverURI.get(currentIndex);

            // Atualiza o índice para o próximo URI
            currentIndex = (currentIndex + 1) % serverURI.size();

            return uri;
    }


}
