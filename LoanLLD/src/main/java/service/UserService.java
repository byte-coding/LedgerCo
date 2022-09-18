package service;


import model.User;

public interface UserService {
    User create(String name);

    User get(String name);
}
