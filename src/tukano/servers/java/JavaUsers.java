package tukano.servers.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


import tukano.api.java.Result;
import tukano.api.java.Result.ErrorCode;
import tukano.api.java.Shorts;
import tukano.api.User;
import tukano.api.java.Users;
import tukano.api.persistence.Hibernate;
import tukano.clients.ShortsClientFactory;


public class JavaUsers implements Users {
	

	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

	/*
	@Override
	public Result<String> createUser(User user) {
		Log.info("createUser : " + user);
		
		// Check if user data is valid
		if(user.userId() == null || user.pwd() == null || user.displayName() == null || user.email() == null) {
			Log.info("User object invalid.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}
		
		// Insert user, checking if name already exists
		if( users.putIfAbsent(user.userId(), user) != null ) {
			Log.info("User already exists.");
			return Result.error( ErrorCode.CONFLICT);
		}
		return Result.ok( user.userId() );
	}*/

	@Override
    public Result<String> createUser(User user) {

        Log.info("Try to createUser : " + user.getUserId());

        // Check if user data is valid
        if(user.getUserId() == null || user.getPwd() == null || user.getDisplayName() == null || user.getEmail() == null) {
            Log.info("User object invalid.");
            return Result.error( Result.ErrorCode.BAD_REQUEST);
        }

        String userId = user.getUserId();
        List<User> exisitingUsers = Hibernate.getInstance().jpql(
            "SELECT u FROM User u WHERE u.userId = :userId",
            Map.of("userId", userId),
            User.class
        );

        // Insert user, checking if name already exists
        if(!exisitingUsers.isEmpty()) {
            Log.info("User already exists.");
            return Result.error( Result.ErrorCode.CONFLICT);
        }

        Hibernate.getInstance().persist(user);
        return Result.ok( user.getUserId() );
    }

	private boolean isObjectValid(String userId, String pwd) {

        if(userId == null || pwd == null) {
            Log.info("User object invalid.");
            return false;
        }
        return true;
    }

    @Override
    public Result<User> getUser(String userId, String pwd) {
        Log.info(userId + " : " + pwd);

        // Check if user data is valid
        if(!isObjectValid(userId,pwd)) {
            return Result.error( Result.ErrorCode.BAD_REQUEST);
        }
        List<User> users = Hibernate.getInstance().jpql(
                "SELECT u FROM User u WHERE u.userId = :userId",
                Map.of("userId", userId),
                User.class
        );

        if (users.isEmpty())    {
            Log.info ("There's no users with this userId");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

        User user = users.get(0);

        if (!user.getPwd().equals(pwd))    {
            Log.info("Wrong Password");
            return Result.error(Result.ErrorCode.FORBIDDEN);
        }

        return Result.ok(user);
    }
	@Override
	public Result<User> updateUser(String userId, String pwd, User user) {

		if (userId == null || pwd == null || user.getUserId() != null)
            return Result.error(ErrorCode.BAD_REQUEST);

		List<User> users = Hibernate.getInstance().jpql(
			"SELECT u FROM User u WHERE u.userId = :userId",
			Map.of("userId", userId),
			User.class
		);

		if (users.isEmpty())    {
            Log.info ("There's no users with this userId");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }

		else {
			User oldUser = users.get(0);
			String oldUserPass = oldUser.pwd();
			if (!oldUserPass.equals(pwd)) {
				Log.info("password does not match.");
				return Result.error(ErrorCode.FORBIDDEN);
			}
			else {
				 // Update user fields if they are not null
				 if (user.getDisplayName() != null) {
					oldUser.setDisplayName(user.getDisplayName());
				}
				if (user.getEmail() != null) {
					oldUser.setEmail(user.getEmail());
				}
				// verificar se é possível alterar o id, visto que é a chave
				if (user.getUserId() != null) {
					oldUser.setUserId(user.getUserId());
				}
				if (user.getPwd() != null) {
					oldUser.setPwd(user.getPwd());
				}
				Log.info("user updated successfully.");
				Hibernate.getInstance().update(oldUser);
				return Result.ok(oldUser);
			}
			//falta bad request
		}
	}

	@Override
	public Result<User> deleteUser(String userId, String pwd) {

		List<User> users = Hibernate.getInstance().jpql(
			"SELECT u FROM User u WHERE u.userId = :userId",
			Map.of("userId", userId),
			User.class
		);

		if (users.isEmpty())    {
            Log.info ("There's no users with this userId");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
		User user = users.get(0);
		String password = user.pwd();
		if (!password.equals(pwd)) {
			Log.info("password does not match.");
			return Result.error(ErrorCode.FORBIDDEN);
		}

		else {
			Shorts shorts = ShortsClientFactory.getClient();
			shorts.removeAllFromUser(userId, pwd);
			Hibernate.getInstance().delete(user);
			return Result.ok(user);
		}

	}

	public Result<List<User>> searchUsers(String pattern) {
        Log.info("Info Received searchUsers : pattern = " + pattern);
        List<User> userList = new ArrayList<>();
        List<User> existing_users= Hibernate.getInstance().sql("SELECT * FROM User", User.class);

        if (pattern.trim().isEmpty() || pattern == null) {
            for(int i = 0; i < existing_users.size(); i++) {
                userList.add(i, existing_users.get(i));
            }
            return Result.ok(userList);
        } else {
            for (User user : existing_users) {
                String userId = user.getUserId();
                if (userId.toLowerCase().contains(pattern.toLowerCase())) {
                    userList.add(user);
                }
            }
            }
        return Result.ok(userList);
    }
}
