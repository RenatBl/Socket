package ru.itis.socket.dao;

import ru.itis.socket.models.User;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class UserDaoImpl implements UserDao {
    private Connection connection;
    private RowMapper<User> userFindRowMapper = row -> {
        Long id = row.getLong("id");

        String name = row.getString("username");
        String password = row.getString("password");
        return new User(id, name, password);
    };

    public UserDaoImpl(Connection connection) {
        this.connection = connection;
    }


    @Override
    public Optional<User> find(Long id) {
        User user = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE id = ?")) {
            statement.setLong(1, id);
            ResultSet resultSet = statement.executeQuery();
            //���� �������������� ������ �������,������������ � c ������� userRowMapper.
            //�������������� �������� ������ User.
            if (resultSet.next()) {
                user = userFindRowMapper.mapRow(resultSet);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(user);
    }

    @Override
    public void save(User model) {
        //������ ����� ������ PreparedStatement,� �������������� �������� ��� ��������� ������������
        //������������� try-with-resources ���������� ��� ���������������� �������� statement,��� ����������� �� ���������� ��������.
        //�������� Statement.RETURN_GENERATED_KEYS ��� ����������� �������� ��������������� id (������)  ������ statement.
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, model.getUserName());
            statement.setString(2, model.getPassword());
            //��������� ������ � ��������� ����������� ��������� �����
            int updRows = statement.executeUpdate();
            if (updRows == 0) {
                //���� ������ �� ���� ��������, ������ �������� ������
                //���������� �������������� ����������
                throw new SQLException();
            }
            //������ ��������� Id ������������
            try (ResultSet set = statement.getGeneratedKeys();) {
                //���� id  ���������,��������� ��� � ������.
                if (set.next()) {
                    model.setId(set.getLong(1));
                } else {
                    //������ ����������� �� �� ������ �������� ��������������� id
                    //���������� ������������� ����������
                    throw new SQLException();
                }
            }

        } catch (SQLException e) {
            //���� ���������� �����������, ������ ��������� ���������� � ������������� � ��������� ������(best-practise)
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void update(User model) {

    }

    @Override
    public void delete(Long id) {

    }

    @Override
    public List<User> findAll() {
        return null;
    }

    @Override
    public Optional<User> findByName(String login) {
        User user = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            statement.setString(1, login);
            ResultSet resultSet = statement.executeQuery();
            //���� �������������� ������ �������,������������ � c ������� userRowMapper.
            //�������������� �������� ������ User.
            if (resultSet.next()) {
                user = userFindRowMapper.mapRow(resultSet);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(user);
    }
}
