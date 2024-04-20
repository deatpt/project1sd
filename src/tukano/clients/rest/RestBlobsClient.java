package tukano.clients.rest;
import java.net.URI;
import org.glassfish.jersey.client.ClientConfig;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import tukano.api.java.Blobs;
import tukano.api.java.Result;
import jakarta.ws.rs.core.Response.Status;
import tukano.api.java.Result.ErrorCode;

import tukano.api.rest.RestBlobs;



@Singleton
public class RestBlobsClient implements Blobs {

    final URI serverURI;
	final Client client;
	final ClientConfig config;

	final WebTarget target;
	
	public RestBlobsClient( URI serverURI ) {
		this.serverURI = serverURI;
		this.config = new ClientConfig();
		this.client = ClientBuilder.newClient(config);
		target = client.target( serverURI ).path( RestBlobs.PATH );
	}


    @Override
    public Result<Void> upload(String blobId, byte[] bytes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'upload'");
    }

    @Override
    public Result<byte[]> download(String blobId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'download'");
    }


    @Override
    public Result<Void> delete(String blobId) {
        Response response = target.path(blobId)
                                       .request()
                                       .delete();
        return handleResponse(response, Void.class);
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
        };}
    

    private <T> Result<T> handleResponse(Response response, Class<T> type) {
        if (response.getStatus() != Status.OK.getStatusCode()) {
            return Result.error(getErrorCodeFrom(response.getStatus()));
        } else {
            return Result.ok(response.readEntity(type));
        }
    }
}
