package tukano.servers.java;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.deser.std.NumberDeserializers.ShortDeserializer;

import tukano.api.Follows;
import tukano.api.Likes;
import tukano.api.Short;
import tukano.api.User;
import tukano.api.java.Blobs;
import tukano.api.java.Result;
import tukano.api.java.Shorts;
import tukano.api.java.Users;
import tukano.api.persistence.Hibernate;
import tukano.clients.BlobsClientFactory;
import tukano.clients.UsersClientFactory;
import tukano.api.java.Result.ErrorCode;


public class JavaShorts implements Shorts{

    private static Logger Log = Logger.getLogger(JavaShorts.class.getName());

    private final Map<String, String> blobIds = new ConcurrentHashMap<>();

    Users usersClient = UsersClientFactory.getClient();

    
    @Override
    public Result<Short> createShort(String userId, String password) {
        
        ErrorCode error = usersClient.getUser(userId, password).error();

        // Validate the password
        if (error == ErrorCode.NOT_FOUND)
            return Result.error(Result.ErrorCode.NOT_FOUND); 

		if (error == ErrorCode.FORBIDDEN)
            return Result.error(Result.ErrorCode.FORBIDDEN);

        if (error == ErrorCode.BAD_REQUEST)
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        
        // Generate a unique identifier for the new short
        // UUID (unique identifier)
        String shortId = UUID.randomUUID().toString();

        String blobId = UUID.randomUUID().toString();

        String blobURL = BlobsClientFactory.getURI() + "/blobs/" + blobId;

        Short short1 = new Short(shortId, userId, blobURL);

        blobIds.put(shortId, blobId);
        // Store the new short
        Hibernate.getInstance().persist(short1);

        // Return the created short
        return Result.ok(short1);
    }
    
    @Override
    public Result<Void> deleteShort(String shortId, String password) {
        Log.info("Deleting short with ID: " + shortId);
    
        // Retrieve the Short with the specified shortId
        List<Short> shorts = Hibernate.getInstance().jpql(
            "SELECT s FROM Short s WHERE s.shortId = :shortId",
            Map.of("shortId", shortId),
            Short.class
        );
    
        // Check if a Short with the specified shortId exists
        if (shorts.isEmpty()) {
            Log.info("Short not found with ID: " + shortId);
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        Short shortToDelete = shorts.get(0);

        String ownerId = shortToDelete.getOwnerId();

        ErrorCode error = usersClient.getUser(ownerId, password).error();

        // Validate the password
        if (error == ErrorCode.FORBIDDEN)
            return Result.error(Result.ErrorCode.FORBIDDEN);
        
        blobIds.remove(shortId);
        // Delete the Short
        Hibernate.getInstance().delete(shortToDelete);
        Log.info("Short deleted successfully with ID: " + shortId);

        return Result.ok(null); // Return OK with void (null)
    }
     

    @Override
    public Result<Short> getShort(String shortId) {
        Log.info("Attempting to retrieve Short for ID: " + shortId);
        
        if (shortId == null) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
    
        List<Short> shorts = Hibernate.getInstance().jpql(
            "SELECT s FROM Short s WHERE s.shortId = :shortId",
            Map.of("shortId", shortId),
            Short.class
        );
    
        if (shorts.isEmpty()) {
            Log.info("No Short found with ID: " + shortId);
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
    
        Short targetShort = shorts.get(0);
        Log.info("Short retrieved successfully for ID: " + shortId);
        return Result.ok(targetShort);
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
         
        Log.info("Retrieving shorts for user ID: " + userId);

        ErrorCode error = usersClient.getUser(userId, "").error();

        // Validate the password
        if (error == ErrorCode.NOT_FOUND)
            return Result.error(Result.ErrorCode.NOT_FOUND);
    
        List<Short> shorts = Hibernate.getInstance().jpql(
            "SELECT s FROM Short s WHERE s.ownerId = :userId",
            Map.of("userId", userId),
            Short.class
        );

        // Extract shortIds from the retrieved shorts
        List<String> shortIds = shorts.stream()
                                      .map(Short::getShortId)
                                      .collect(Collectors.toList());
    
        // Check if shorts list is empty
    
        Log.info("Shorts retrieved successfully for user ID: " + userId);
        return Result.ok(shortIds);
    }
    

    @Override
    public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {
        Log.info("Processing follow request from user ID: " + userId1 + " to user ID: " + userId2);

        // Authenticate userId1 with the provided password
        Result<User> userResult = usersClient.getUser(userId1, password);

        // Check if userId1 exists and password is correct
        if (userResult.error() == ErrorCode.NOT_FOUND) {
            Log.info("User ID: " + userId1 + " not found.");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else if (userResult.error() == ErrorCode.FORBIDDEN) {
            Log.info("Incorrect password for user ID: " + userId1);
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        // Check if userId2 exists
        List<User> user2Result = usersClient.searchUsers(userId2).value();

        //TODO check if really the hits of the list is equal to userid2

        // Check if userId2 exists
        if (user2Result.isEmpty()) {
            Log.info("User ID: " + userId2 + " not found.");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        Follows followObj = new Follows(userId1, userId2);
        Follows existingFollow = getFollowObj(userId1, userId2);
        // Perform the follow or unfollow operation based on isFollowing flag
        if (isFollowing) {
            if (existingFollow == null)  
                Hibernate.getInstance().persist(followObj);
            else {
                return Result.error(Result.ErrorCode.CONFLICT);
            }
        } else {
            Hibernate.getInstance().delete(followObj);
            Log.info("User ID: " + userId1 + " is ceasing to follow user ID: " + userId2);
        }
        
        // Return success (OK) response
        return Result.ok(null);
    }

 

    @Override
    public Result<List<String>> followers(String userId, String password) {
         
        Log.info("Retrieving followers for user ID: " + userId);

        // Authenticate the followed user with the provided password
        Result<User> userResult = usersClient.getUser(userId, password);

        if (userResult.error() == ErrorCode.NOT_FOUND) {
            Log.info("User ID: " + userId + " not found.");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else if (userResult.error() == ErrorCode.FORBIDDEN) {
            Log.info("Incorrect password for user ID: " + userId);
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        // Retrieve followers of the specified user from the database using Hibernate
        List<String> followerIds = new ArrayList<>();
        
        List<Follows> followerRelations = Hibernate.getInstance().jpql(
            "SELECT f FROM Follows f WHERE f.followedId = :userId",
            Map.of("userId", userId),
            Follows.class
        );

        for (Follows follow : followerRelations) {
            followerIds.add(follow.getFollowerId());
        }
        return Result.ok(followerIds);
    }


    @Override
    public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
        Log.info("Processing like request for short ID: " + shortId + " by user ID: " + userId);
        
        Result<User> userResult = usersClient.getUser(userId, password);

        if (userResult.error() == ErrorCode.FORBIDDEN) {
                Log.info("Incorrect password for user ID: " + userId);
                return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        List<Short> shorts = Hibernate.getInstance().jpql("SELECT s FROM Short s WHERE s.shortId = :shortId",
        Map.of("shortId", shortId),
        Short.class);

        Short targetShort = shorts.get(0);

        if (targetShort == null) {
            Log.info("Short ID: " + shortId + " not found.");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        List<Likes> likeList = Hibernate.getInstance().jpql(
            "SELECT l FROM Likes l WHERE l.shortId = :shortId AND l.userId = :userId",
            Map.of("shortId", shortId, "userId", userId),
            Likes.class
        );

        if (!likeList.isEmpty()){
            Likes likeObj = likeList.get(0);

            if (isLiked) {
                Log.info("Like already exists for short ID: " + shortId + " by user ID: " + userId);
                return Result.error(Result.ErrorCode.CONFLICT);
            } else {
                targetShort.setTotalLikes(targetShort.getTotalLikes() - 1);
                Hibernate.getInstance().update(targetShort);
                Hibernate.getInstance().delete(likeObj);
                Log.info("Like removed for short ID: " + shortId + " by user ID: " + userId);
            }
        } else {
            if (isLiked) {
                Likes like = new Likes(userId, shortId);
                targetShort.setTotalLikes(targetShort.getTotalLikes() + 1);
                Hibernate.getInstance().update(targetShort);
                Hibernate.getInstance().persist(like);
                Log.info("User ID: " + userId + " liked short ID: " + shortId);
            } else {
                Log.info("No like found to remove for short ID: " + shortId + " by user ID: " + userId);
                return Result.error(Result.ErrorCode.NOT_FOUND);
            }
        }

        // Return success (OK) response
        return Result.ok(null);
}


    @Override
    public Result<List<String>> likes(String shortId, String password) {

        List<Short> shorts = Hibernate.getInstance().jpql("SELECT s FROM Short s WHERE s.shortId = :shortId",
        Map.of("shortId", shortId),
        Short.class);

        Short targetShort = shorts.get(0);

        if (targetShort == null) {
            Log.info("Short ID: " + shortId + " not found.");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
    
        Result<User> userResult = usersClient.getUser(targetShort.getOwnerId(), password);

        if (userResult.error() == ErrorCode.FORBIDDEN) 
            return Result.error(Result.ErrorCode.FORBIDDEN);

        List<String> likerIds = new ArrayList<>();

        List<Likes> likes = Hibernate.getInstance().jpql(
            "SELECT l FROM Likes l WHERE l.shortId = :shortId",
            Map.of("shortId", shortId),
            Likes.class
        );
        
        for (Likes like : likes) {
            likerIds.add(like.getUserId());
        }
        
        return Result.ok(likerIds);
    }

    @Override
    public Result<List<String>> getFeed(String userId, String password) {
        
        Log.info("Retrieving feed for user ID: " + userId);

        Result<User> userResult = usersClient.getUser(userId, password);

        if (userResult.error() == ErrorCode.NOT_FOUND) {
            Log.info("User ID: " + userId + " not found.");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else if (userResult.error() == ErrorCode.FORBIDDEN) {
            Log.info("Incorrect password for user ID: " + userId);
            return Result.error(Result.ErrorCode.FORBIDDEN); //FORBIDDEN
        }

        // Retrieve own shorts
        List<Short> ownShorts = Hibernate.getInstance().jpql(
            "SELECT s FROM Short s WHERE s.ownerId = :userId",
            Map.of("userId", userId),
            Short.class
        );

        List<Short> sortedShorts = ownShorts.stream()
        .sorted(Comparator.comparingLong(Short::getTimestamp).reversed())
        .collect(Collectors.toList());

        // Get the list of users followed by the authenticated user
        List<String> followedUserIds = getFollowedUserIds(userId);
        if (followedUserIds.isEmpty()) {
            Log.info("No followed users found for user ID: " + userId);
            List<String> ownShortsSortedStr = extractShortIds(sortedShorts);
            return Result.ok(ownShortsSortedStr); // Return empty list if no followed users
        }
        else{
        // Retrieve shorts from followed users
        List<Short> feedShorts = new ArrayList<>();
        for (String followedUserId : followedUserIds) {
            List<Short> shortsByFollowedUser = getShortsByUser(followedUserId);
            feedShorts.addAll(shortsByFollowedUser);
        }

        feedShorts.addAll(ownShorts);

        List<Short> feedShortsSorted = feedShorts.stream()
        .sorted(Comparator.comparingLong(Short::getTimestamp).reversed()).collect(Collectors.toList());
        

        // Extract shortIds from feedShorts
        List<String> feedShortIds = extractShortIds(feedShortsSorted);

        Log.info("Retrieved feed successfully for user ID: " + userId);
        
        return Result.ok(feedShortIds);
        }
    }

    
    public Result<Void> removeAllFromUser(String userId, String password) {
        removeLikesByUser(userId, password);
        removeShortsByOwner(userId, password);
        removeFollowedByUser(userId);
        return Result.ok();
    }


    
    // Helper method to extract shortIds from a list of Short objects
    private List<String> extractShortIds(List<Short> shorts) {
        return shorts.stream()
            .map(Short::getShortId)
            .collect(Collectors.toList());
    }

    // Helper method to get the list of user IDs followed by the specified user
    private List<String> getFollowedUserIds(String userId) {
        List<String> followedUserIds = new ArrayList<>();
        try {
            List<Follows> followsList = Hibernate.getInstance().jpql(
                "SELECT f FROM Follows f WHERE f.followerId = :userId",
                Map.of("userId", userId),
                Follows.class
            );
            followedUserIds = followsList.stream().map(Follows::getFollowedId).collect(Collectors.toList());
        } catch (Exception e) {
            Log.severe("Error retrieving followed users for user ID: " + userId + ". " + e.getMessage());
        }
        return followedUserIds;
    }

    // Helper method to get the list of shorts created by the specified user
    private List<Short> getShortsByUser(String userId) {
        List<Short> shorts = new ArrayList<>();
            shorts = Hibernate.getInstance().jpql(
                "SELECT s FROM Short s WHERE s.ownerId = :userId",
                Map.of("userId", userId),
                Short.class
            );
        return shorts;
    }

    private Follows getFollowObj(String followerId, String followedId) {
        List<Follows> followsList = Hibernate.getInstance().jpql(
            "SELECT f FROM Follows f WHERE f.followerId = :followerId AND f.followedId = :followedId",
            Map.of("followerId", followerId, "followedId", followedId),
            Follows.class
        );
        
        // Check if the query returned any results
        if (!followsList.isEmpty()) {
            // Return the first (and hopefully only) result
            return followsList.get(0);
        }
        
        // If no matching Follows entity found, return null
        return null;
    }

    private void removeLikesByUser(String userId, String password) {
        // Remove all likes from shorts where the user was the one who liked
        List<Likes> likesToRemove = Hibernate.getInstance().jpql(
            "SELECT l FROM Likes l WHERE l.userId = :userId",
            Map.of("userId", userId),
            Likes.class
        );

        for (Likes like : likesToRemove) {
            like(like.getShortLikedId(), userId, false, password);
        }
        
    }

    private void removeShortsByOwner(String ownerId, String password) {
        // Remove all shorts from the database that the user owns
        List<Short> shortsToRemove = Hibernate.getInstance().jpql(
            "SELECT s FROM Short s WHERE s.ownerId = :ownerId",
            Map.of("ownerId", ownerId),
            Short.class
        );

        for (int i = 0; i < shortsToRemove.size(); i++) {
            Short short1 = shortsToRemove.get(i);
            String blobURL = short1.getBlobUrl();
            String[] parts = blobURL.split("/");
            String blobId = parts[parts.length - 1];
            Blobs blobs = BlobsClientFactory.getClient();
            blobs.delete(blobId);
            blobIds.remove(short1.getShortId());
            deleteShort(short1.getShortId(), password);
        }


    }

    private void removeFollowedByUser(String userId) {
        // Remove all entries where the user is being followed
        List<Follows> followsToRemove = Hibernate.getInstance().jpql(
            "SELECT f FROM Follows f WHERE f.followedId = :userId",
            Map.of("userId", userId),
            Follows.class
        );
        followsToRemove.forEach(follow -> Hibernate.getInstance().delete(follow));
    }

    @Override
    public Result<Void> isBlobInMap(String blobId) {
        if (!blobIds.containsValue(blobId)) {
            return Result.error(ErrorCode.NOT_FOUND);
        } else {
            return Result.ok();
        }
    }




}

