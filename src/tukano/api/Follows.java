package tukano.api;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Follows {
    @Id
    private String followerId;
    
    @Id
    private String followedId;

    // Default constructor (not really needed if no additional logic is required)
    public Follows() {
    }

    // Parameterized constructor
    public Follows(String followerId, String followedId) {
        this.followerId = followerId;
        this.followedId = followedId;
    }

    // Getter for followerId
    public String getFollowerId() {
        return followerId;
    }

    // Getter for followedId
    public String getFollowedId() {
        return followedId;
    }

}