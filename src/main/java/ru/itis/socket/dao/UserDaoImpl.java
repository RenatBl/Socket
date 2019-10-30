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
            //Если соответстующая строка найдена,обрабатываем её c помощью userRowMapper.
            //Соответствунно получаем объект User.
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
        //Создаём новый объект PreparedStatement,с соотвествующим запросом для сохранния пользователя
        //Использование try-with-resources необходимо для гарантированного закрытия statement,вне зависимости от успешности операции.
        //Аргумент Statement.RETURN_GENERATED_KEYS даёт возможность хранения сгенерированных id (ключей)  внутри statement.
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, model.getUserName());
            statement.setString(2, model.getPassword());
            //Выполняем запрос и сохраняем колличество изменённых строк
            int updRows = statement.executeUpdate();
            if (updRows == 0) {
                //Если ничего не было изменено, значит возникла ошибка
                //Возбуждаем соответсвующее исключений
                throw new SQLException();
            }
            //Достаём созданное Id пользователя
            try (ResultSet set = statement.getGeneratedKeys();) {
                //Если id  существет,обновляем его у подели.
                if (set.next()) {
                    model.setId(set.getLong(1));
                } else {
                    //Модель сохранилась но не удаётся получить сгенерированный id
                    //Возбуждаем соответвующее исключение
                    throw new SQLException();
                }
            }

        } catch (SQLException e) {
            //Если сохранений провалилось, обернём пойманное исключение в непроверяемое и пробросим дальше(best-practise)
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
            //Если соответстующая строка найдена,обрабатываем её c помощью userRowMapper.
            //Соответствунно получаем объект User.
            if (resultSet.next()) {
                user = userFindRowMapper.mapRow(resultSet);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(user);
    }
}
