package tukano.api;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Likes {
    @Id
    private String userId;  // The ID of the user who likes something
    
    @Id
    private String shortId;  // The ID of the item that is liked by the user

    // Default constructor (not really needed if no additional logic is required)
    public Likes() {
    }

    // Parameterized constructor
    public Likes(String userId, String shortId) {
        this.userId = userId;
        this.shortId = shortId;
    }

    // Getter for userId
    public String getUserId() {
        return userId;
    }

    // Getter for likedItemId
    public String getShortLikedId() {
        return shortId;
    }
}

