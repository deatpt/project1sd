package tukano.servers.rest;

import java.util.List;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tukano.api.Short;
import tukano.api.java.Result;
import tukano.api.java.Shorts;
import tukano.api.rest.RestShorts;
import tukano.servers.java.JavaShorts;

@Singleton
public class RestShortsResource implements RestShorts{

    final Shorts impl;
	public RestShortsResource() {
		this.impl = new JavaShorts();
	}
    
    @Override
    public Short createShort(String userId, String password) {
        return resultOrThrow(impl.createShort(userId, password));
    }

    @Override
    public void deleteShort(String shortId, String password) {
        resultOrThrow(impl.deleteShort(shortId, password));
    }

    @Override
    public Short getShort(String shortId) {
        return resultOrThrow(impl.getShort(shortId));
    }

    @Override
    public List<String> getShorts(String userId) {
        return resultOrThrow(impl.getShorts(userId));
    }

    @Override
    public void follow(String userId1, String userId2, boolean isFollowing, String password) {
        resultOrThrow(impl.follow(userId1, userId2, isFollowing, password));
    }

    @Override
    public List<String> followers(String userId, String password) {
        return resultOrThrow(impl.followers(userId, password));
    }

    @Override
    public void like(String shortId, String userId, boolean isLiked, String password) {
        resultOrThrow(impl.like(shortId, userId, isLiked, password));
    }

    @Override
    public List<String> likes(String shortId, String password) {
        return resultOrThrow(impl.likes(shortId, password));
    }

    @Override
    public List<String> getFeed(String userId, String password) {
        return resultOrThrow(impl.getFeed(userId, password)); 
    }

    @Override
    public void removeAllFromUser(String userId, String password) {
        resultOrThrow(impl.removeAllFromUser(userId, password));
    }

    /**
	 * Given a Result<T>, either returns the value, or throws the JAX-WS Exception
	 * matching the error code...
	 */
	protected <T> T resultOrThrow(Result<T> result) {
		if (result.isOK())
			return result.value();
		else
			throw new WebApplicationException(statusCodeFrom(result));
	}

	/**
	 * Translates a Result<T> to a HTTP Status code
	 */
	private static Status statusCodeFrom(Result<?> result) {
		return switch (result.error()) {
			case CONFLICT -> Status.CONFLICT;
			case NOT_FOUND -> Status.NOT_FOUND;
			case FORBIDDEN -> Status.FORBIDDEN;
			case BAD_REQUEST -> Status.BAD_REQUEST;
			case INTERNAL_ERROR -> Status.INTERNAL_SERVER_ERROR;
			case NOT_IMPLEMENTED -> Status.NOT_IMPLEMENTED;
			case OK -> result.value() == null ? Status.NO_CONTENT : Status.OK;
			default -> Status.INTERNAL_SERVER_ERROR;
		};
	}

    @Override
    public void blobStillExist(String blobId) throws InterruptedException {
        resultOrThrow(impl.isBlobInMap(blobId));
    }
    
}
