package ru.itis.socket.dao;

import ru.itis.socket.models.Message;

import java.util.List;

public interface MessageDao extends CrudDao<Message> {
    List<Message> findAllById(Long id, int limit, boolean foreign_key);
}
