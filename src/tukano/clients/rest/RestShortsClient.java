package tukano.clients.rest;

import java.net.URI;
import java.util.List;

import org.glassfish.jersey.client.ClientConfig;

import jakarta.inject.Singleton;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tukano.api.java.Result;
import tukano.api.rest.RestShorts;
import tukano.api.java.Result.ErrorCode;
import tukano.api.java.Shorts;
import tukano.api.Short;


@Singleton
public class RestShortsClient implements Shorts {

    final URI serverURI;
	final Client client;
	final ClientConfig config;

	final WebTarget target;
	
	public RestShortsClient( URI serverURI ) {
		this.serverURI = serverURI;
		this.config = new ClientConfig();
		this.client = ClientBuilder.newClient(config);

		target = client.target( serverURI ).path( RestShorts.PATH );
	}

    @Override
    public Result<Short> createShort(String userId, String password) { 
        Response response = target.path(userId)
                                  .queryParam(RestShorts.PWD, password)
                                  .request()
                                  .post(Entity.entity(userId, MediaType.APPLICATION_JSON)); 
        return handleResponse(response, Short.class);
    }

    @Override
    public Result<Void> deleteShort(String shortId, String password) {
        Response response = target.path(shortId)
                                       .queryParam(RestShorts.PWD, password)
                                       .request()
                                       .delete();
        return handleResponse(response, Void.class);
    }

    @Override
    public Result<Short> getShort(String shortId) {
        Response response = target.path(shortId)
                                  .request(MediaType.APPLICATION_JSON)
                                  .get();
        return handleResponse(response, Short.class);
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        Response response = target.path(userId).path(RestShorts.SHORTS).request().accept(MediaType.APPLICATION_JSON).get();

        int status = response.getStatus();
        if (status == Status.OK.getStatusCode()) {
            List<String> shortIds = response.readEntity(new GenericType<List<String>>() {
            });
            return Result.ok(shortIds);
        } else {
            ErrorCode errorCode = getErrorCodeFrom(status);
            return Result.error(errorCode);
        }
    }

    @Override
    public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {
        WebTarget followTarget = target.path(userId1 + "/" + userId2 + "/followers").queryParam(RestShorts.PWD,
                password);

        // Prepare the JSON body for the POST request
        String json = "{\"isFollowing\":" + isFollowing + "}";

        Response response = followTarget.request(MediaType.APPLICATION_JSON).post(Entity.json(json));

        int status = response.getStatus();
        if (status == Status.OK.getStatusCode()) {
            return Result.ok();
        } else {
            ErrorCode errorCode = getErrorCodeFrom(status);
            return Result.error(errorCode);
        }

    }
    @Override
    public Result<List<String>> followers(String userId, String password) {
        Response response = target.path(userId).queryParam(RestShorts.PWD, password).request()
                .accept(MediaType.APPLICATION_JSON).get();

        int status = response.getStatus();
        if (status == Status.OK.getStatusCode()) {
            // Read the followers from the response
            List<String> followers = response.readEntity(new GenericType<List<String>>() {
            });
            return Result.ok(followers);
        } else {
            // If there was an error, return the appropriate error code
            ErrorCode errorCode = getErrorCodeFrom(status);
            return Result.error(errorCode);
        }
    }

    @Override
    public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {

        WebTarget likeTarget = target.path(shortId + "/" + userId + "/likes").queryParam(RestShorts.PWD, password);

        // Prepare the JSON body for the POST request
        String json = "{\"isLiked\":" + isLiked + "}";

        Response response = likeTarget.request(MediaType.APPLICATION_JSON).post(Entity.json(json));

        int status = response.getStatus();
        if (status == Status.OK.getStatusCode()) {
            return Result.ok();
        } else {
            // If there was an error, return the appropriate error code
            ErrorCode errorCode = getErrorCodeFrom(status);
            return Result.error(errorCode);
        }
    }

    @Override
    public Result<List<String>> likes(String shortId, String password) {
        Response response = target.path(shortId).path("likes").queryParam(RestShorts.PWD, password).request()
                .accept(MediaType.APPLICATION_JSON).get();

        int status = response.getStatus();
        if (status == Status.OK.getStatusCode()) {
            // Read the likes from the response
            List<String> likes = response.readEntity(new GenericType<List<String>>() {
            });
            return Result.ok(likes);
        } else {
            // If there was an error, return the appropriate error code
            ErrorCode errorCode = getErrorCodeFrom(status);
            return Result.error(errorCode);
        }
    }

    @Override
    public Result<List<String>> getFeed(String userId, String password) {
        Response response = target.path(userId).path("feed")
                .queryParam(RestShorts.PWD, password)
                .request(MediaType.APPLICATION_JSON).get();

        int status = response.getStatus();
        if (status == Status.OK.getStatusCode()) {
            List<String> feedShorts = response.readEntity(new GenericType<List<String>>() {
            });
            return Result.ok(feedShorts);
        } else {
            ErrorCode errorCode = getErrorCodeFrom(status);
            return Result.error(errorCode);
        }
    }

    public Result<Void> removeAllFromUser(String userId, String password) {
        Response response = target.path(userId + RestShorts.REMOVEALLFROMUSER).queryParam(RestShorts.PWD, password)
                                       .request()
                                       .delete();
        return handleResponse(response, Void.class);
    }


    private <T> Result<T> handleResponse(Response response, Class<T> type) {
        if (response.getStatus() != Status.OK.getStatusCode()) {
            return Result.error(getErrorCodeFrom(response.getStatus()));
        } else {
            return Result.ok(response.readEntity(type));
        }
    }

    public static ErrorCode getErrorCodeFrom(int status) {
        return switch (status) {
            case 200, 209 -> ErrorCode.OK;
            case 409 -> ErrorCode.CONFLICT;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 400 -> ErrorCode.BAD_REQUEST;
            case 500 -> ErrorCode.INTERNAL_ERROR;
            case 501 -> ErrorCode.NOT_IMPLEMENTED;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }

    @Override
    public Result<Void> isBlobInMap(String blobId) {
        for (int i = 0; i < 3; i++) {
            try {
                Response response = target.path(blobId).request(MediaType.APPLICATION_JSON).get();

                int status = response.getStatus();
                if (status == Status.OK.getStatusCode()) {
                    return Result.ok();
                } else {
                    ErrorCode errorCode = getErrorCodeFrom(status);
                    return Result.error(errorCode);
                }
            } catch (ProcessingException x) {
                utils.Sleep.ms(1000);
            } catch (Exception x) {
                x.printStackTrace();
            }

        }
        return Result.error(ErrorCode.TIMEOUT);
    }
    
}
