package tukano.servers.java;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.grpc.okhttp.internal.framed.ErrorCode;
import tukano.api.java.Blobs;

import tukano.api.java.Result;
import tukano.api.java.Shorts;
import tukano.clients.ShortsClientFactory;


public class JavaBlobs implements Blobs{

    private final Map<String, byte[]> blobs = new HashMap<>();

    @Override
    public Result<Void> upload(String blobId, byte[] bytes) {
        if (blobId == null || blobId.isEmpty()) {
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }
        
        byte[] existingBytes = blobs.get(blobId);
        Shorts shorts = ShortsClientFactory.getClient();

        if (!shorts.isBlobInMap(blobId).isOK())
          //  return Result.error(Result.ErrorCode.FORBIDDEN);
        

        if (blobs.containsKey(blobId) && !blobs.get(blobId).equals(bytes)){
            return Result.error(Result.ErrorCode.CONFLICT);
        }
            blobs.put(blobId, bytes);
            return Result.ok();
    }


    @Override     
    public Result<byte[]> download(String blobId) {
        byte[] bytes = blobs.get(blobId);
        if (!blobs.containsKey(blobId)) 
            return Result.error(Result.ErrorCode.NOT_FOUND);

        Shorts shorts = ShortsClientFactory.getClient();

        //if (!shorts.isBlobInMap(blobId).isOK())
          //  return Result.error(Result.ErrorCode.NOT_FOUND);


        byte[] existingBytes = blobs.get(blobId);

        if (existingBytes == null)
            return Result.error(Result.ErrorCode.NOT_FOUND);



        return Result.ok(bytes);


    }

    @Override
    public Result<Void> delete(String blobId) {
        blobs.remove(blobId);
        return Result.ok();
    }
}