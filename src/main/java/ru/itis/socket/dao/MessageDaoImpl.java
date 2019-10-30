package ru.itis.socket.dao;

import ru.itis.socket.models.Message;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MessageDaoImpl implements MessageDao {
    private Connection connection;

    public MessageDaoImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Message> find(Long id) {
        return Optional.empty();
    }

    @Override
    public void save(Message model) {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO message (text, date, owner_id,receiver) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, model.getText());
            statement.setObject(2, LocalDateTime.now());
            statement.setLong(3, model.getOwnerId());
            if (model.getReceiverId() == null) {
                statement.setLong(4, (-1));
            } else {
                statement.setLong(4, model.getReceiverId());
            }
            int updRows = statement.executeUpdate();
            if (updRows == 0) {
                throw new SQLException();
            }
            try (ResultSet set = statement.getGeneratedKeys()) {
                if (set.next()) {
                    model.setId(set.getLong(1));
                } else {
                    throw new SQLException();
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void update(Message model) {

    }

    @Override
    public void delete(Long id) {

    }

    @Override
    public List<Message> findAll() {
        return null;
    }

    private RowMapper<Message> messageRowMapper = row -> {
        Long id = row.getLong("id");

        String text = row.getString("text");
        LocalDateTime date = row.getObject(3, LocalDateTime.class);
        Long owner_id = row.getLong(4);
        Long receiver = row.getLong(5);
        return new Message(id, text, date, owner_id, receiver);
    };

    @Override
    public List<Message> findAllById(Long id, int limit, boolean foreign_key) {
        List<Message> result = new ArrayList<>();

        String SQL_findById = "select * from message WHERE receiver = '" + id + "'" + " ORDER BY id DESC " + " LIMIT "
                + limit + " ;";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(SQL_findById)) {
            while (resultSet.next()) {
                Message message = messageRowMapper.mapRow(resultSet);
                result.add(message);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }
}
