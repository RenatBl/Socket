package ru.itis.socket.dao;

import ru.itis.socket.models.User;

import java.util.Optional;

public interface UserDao extends CrudDao<User> {
    Optional<User> findByName(String login);
}
