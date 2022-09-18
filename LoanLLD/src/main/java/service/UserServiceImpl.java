package service;

import model.User;

import java.util.HashMap;
import java.util.Map;

public class UserServiceImpl implements UserService {
    private Map<String, User> userMap = new HashMap<>();

    @Override
    public User create(String name) {
        if (userMap.containsKey(name)) {
            return userMap.get(name);
        }
        userMap.put(name, new User(name));
        return userMap.get(name);
    }

    @Override
    public User get(String name) {
        if (!userMap.containsKey(name)) {
            throw new RuntimeException("User Not Found!");
        }
        return userMap.get(name);
    }
}
