package tukano.clients;

import java.net.URI;

import tukano.Discovery;
import tukano.api.java.Users;
import tukano.clients.rest.RestUsersClient;

public class UsersClientFactory {

    public static Users getClient() {
        Discovery discovery = Discovery.getInstance();
        var svURIs = discovery.knownUrisOf("users", 1);
        URI serverURI =  svURIs.get(0);

        if( serverURI.toString().endsWith("rest"))
          return new RestUsersClient(serverURI); // Discover the URI dynamically
        else {
            return null; // Handle case where URI discovery fails
        }
    }
}