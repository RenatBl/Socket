package ru.itis.socket.servers;

import ru.itis.socket.dao.MessageDao;
import ru.itis.socket.dao.MessageDaoImpl;
import ru.itis.socket.dao.UserDao;
import ru.itis.socket.dao.UserDaoImpl;
import ru.itis.socket.models.Message;
import ru.itis.socket.models.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Date;
import java.sql.DriverManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatMultiServer {
    // список клиентов
    private List<ClientHandler> clients;
    private String[] properties;

    public ChatMultiServer(String[] properties) {
        this.properties = properties;
// Список для работы с многопоточностью
        clients = new CopyOnWriteArrayList<>();
    }

    public void start(int port) {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
// запускаем бесконечный цикл
        while (true) {
            try {
                new ClientHandler(serverSocket.accept()).start();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private class ClientHandler extends Thread {
        // связь с одним клиентом
        private Socket clientSocket;
        private BufferedReader in;
        private UserDao userDao;
        private MessageDao messageDao;
        public User user;

        ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                System.out.println("New user connection");
                this.userDao = new UserDaoImpl(DriverManager.getConnection(properties[0], properties[1], properties[2]));
                this.messageDao = new MessageDaoImpl(DriverManager.getConnection(properties[0], properties[1], properties[2]));
                // получем входной поток для конкретного клиента
                in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                checkAuth();
                loadLast30Messages();
                boolean flag = true;
                while ((inputLine = in.readLine()) != null && flag) {
                    LocalDateTime now = LocalDateTime.now();
                    long messageCase = saveMessage(inputLine, now);
                    String caseParam = "" + messageCase;
                    switch (caseParam) {
                        case ("2"):
                            continue;
                        case ("3"):
                            String resultLine = this.constructResultLine(inputLine, now);
                            if (".".equals(inputLine)) {
                                notifyClients(resultLine);
                                clients.remove(this);
                                stopConnection();
                                flag = false;
                                break;
                            }
                            notifyClients(resultLine);
                            break;
                        default:
                            String result = constructResultLine(inputLine.split("@@@"), now);
                            notifyClient(result, messageCase);
                            break;
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private void notifyClient(String result, long messageCase) throws IOException {
            boolean flag = false;
            for (ClientHandler client : clients) {
                if (client.user.getId() == messageCase) {
                    PrintWriter out = new PrintWriter(client.clientSocket.getOutputStream(), true);
                    out.println(result);
                    flag = true;
                    break;
                } else {
                    continue;
                }
            }
            if (!flag) {
                new PrintWriter(this.clientSocket.getOutputStream()).println("Пользователь неактивен," +
                        "он получит Ваше сообщение, когда зайдёт в беседу в следующий раз");
            }
        }

        private long saveMessage(String inputLine, LocalDateTime now) throws IOException {
            if (inputLine.contains("@@@")) {
                String[] strings = inputLine.split("@@@");
                Optional<User> userRec = userDao.findByName(strings[0]);
                if (userRec.isPresent()) {
                    messageDao.save(new Message(strings[1], now, this.user.getId(), userRec.get().getId()));
                    return userRec.get().getId();
                } else {
                    new PrintWriter(this.clientSocket.getOutputStream()).println("Такого пользователя не сущесвует, попробуйте ещё раз");
                    return 2;
                }
            } else {
                messageDao.save(new Message(inputLine, now, this.user.getId()));
                return 3;
            }
        }

        private String constructResultLine(String inputLine, LocalDateTime now) {
            Instant current = now.toInstant(ZoneOffset.UTC);
            if (inputLine.equals(".")) {
                return "" + this.user.getUserName() + " on " + Date.from(current) + " : " + "Bye";
            }
            return "" + this.user.getUserName() + " on " + Date.from(current) + " : " + inputLine;
        }

        private String constructResultLine(String[] strings, LocalDateTime now) {
            Instant current = now.toInstant(ZoneOffset.UTC);
            return "" + this.user.getUserName() + " on " + Date.from(current) + " : " + "(private message) " + strings[1];
        }

        private void loadLast30Messages() throws IOException {
            List<Message> messages = messageDao.findAllById(this.user.getId(), 25, true);
            //Message message = null;
            if (messages.size() != 0) {
                //message = messages.get(messages.size() - 1);
                printAfterLogin(messages, true);
            }
            Long id = new Long(-1);
            messages = messageDao.findAllById(id, 10, true);
            if (messages.size() != 0) {
                //messages.stream().filter(message1 -> message1.getDateTime().isAfter(message.getDateTime()));
                printAfterLogin(messages, false);
            }
        }

        private void printAfterLogin(List<Message> messages, boolean param) throws IOException {
            PrintWriter pw = new PrintWriter(this.clientSocket.getOutputStream(), true);
            if (param) {
                pw.println("private messages");
            } else {
                pw.println("public chat messages");
            }
            for (Message message1 : messages) {
                Optional<User> user = userDao.find(message1.getOwnerId());
                Instant current = message1.getDateTime().toInstant(ZoneOffset.UTC);
                if (param) {
                    pw.println("" + user.get().getUserName() + " on " + Date.from(current) +
                            " : " + "(private message) " + message1.getText());
                } else {
                    pw.println("" + user.get().getUserName() + " on " + Date.from(current) +
                            " : " + message1.getText());
                }
            }
        }

        private void stopConnection() throws IOException {
            if (this.user != null) {
                System.out.println("Connection stopped on" + this.user.getUserName()
                        + " user with id " + this.user.getId());
            } else {
                System.out.println("Connection stopped without authorization");
            }
            this.clientSocket.close();
            in.close();
            this.stop();
        }

        private void notifyClients(String inputLine) throws IOException {
            for (ClientHandler client : clients) {
                PrintWriter out = new PrintWriter(client.clientSocket.getOutputStream(), true);
                out.println(inputLine);
            }
        }

        private void checkAuth() {
            try {
                PrintWriter out = new PrintWriter(this.clientSocket.getOutputStream(), true);
                String input;
                boolean flag = false;
                while (!flag) {
                    input = this.getUserCase(out, in);
                    switch (input) {
                        case "1":
                            flag = registration(out);
                            if (flag) {
                                flag = login(out);
                            }
                            break;
                        case "2":
                            flag = login(out);
                            break;
                        case "3":
                            this.stopConnection();
                            break;
                        default:
                            out.println("Неверный выбор)) Попробуйте ещё раз");
                            break;
                    }
                }
                out.println("Если Вы хотите отправлять сообщения только конечному адресанту, ");
                out.println("Вводите их по примеру: login@@@message");
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        private String getUserCase(PrintWriter out, BufferedReader in) throws IOException {
            out.println("1 - зарегистрироваться,2 - авторизироваться, 3 - !!!!!завершить подключение");
            return in.readLine();
        }

        private boolean login(PrintWriter out) throws IOException {
            out.println("Авторизация");
            boolean flag = false;
            String login;
            String password;
            while (!flag) {
                out.println("Введите логин");
                login = this.in.readLine();
                if (login.equals("3")) {
                    return false;
                }
                boolean check = checkIfUsersLogin(login);
                if (!check) {
                    Optional<User> user = userDao.findByName(login);
                    if (user.isPresent()) {
                        out.println("Введите пароль");
                        password = this.in.readLine();
                        if (user.get().getPassword().equals(password)) {
                            this.user = user.get();
                            clients.add(this);
                            return true;
                        } else {
                            out.println("Неверный пароль, попробуйте ещё разок или введите 3, чтобы выйти");
                        }
                    } else {
                        out.println("Такого пользователя нет, попробуйте ещё раз, или введите 3, чтобы выйти");
                    }
                } else {
                    out.println("Данный пользователь уже залогинен, попробуйте ещё раз, или введите 3, чтобы выйти");
                }
            }
            return true;
        }

        private boolean checkIfUsersLogin(String login) {
            for (ClientHandler clientHandler : clients) {
                if (clientHandler.user.getUserName().equals(login)) {
                    return true;
                }
            }
            return false;
        }

        private boolean registration(PrintWriter out) throws IOException {
            out.println("Регистрация");
            out.println("Введите логин");
            boolean flag = false;
            String login;
            while (!flag) {
                login = this.in.readLine();
                if (userDao.findByName(login).isPresent()) {
                    out.println("Логин существует, попробуйте ещё раз, или выйдите, введя exit");
                } else {
                    flag = true;
                    if (login.equals("exit")) {
                        return false;
                    }
                    out.println("Введите пароль");
                    String password = this.in.readLine();
                    userDao.save(new User(login, password));
                }
            }
            return true;
        }
    }
}