package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class Server {
    // задаю порт для подключения
    private static final int PORT = 8189;
    // коллекция из всех соединений
    private static List<MyServer> clients;

    private static Authentication auth;

    public static void main(String[] args) throws IOException {
        clients = new Vector<>();
        auth = new UsersAuth();

        try (ServerSocket server = new ServerSocket(PORT)) {
            // создание сервера
            System.out.println("Server has started running");

            while (true) {
                // прикрепление клиента
                Socket client = server.accept();
                // закрепление ссылки на новый сервер
                MyServer create = new MyServer(client);
                // добавить клиента в коллекцию
                //addConnection(create);
                // открыть новый поток для нового подключения
                new Thread(create).start();
            }
        }
    }

    // метод для отправки общих сообщений на все соединения
    public static void sendAll (MyServer sender, String msg) throws IOException {
        String message = String.format("%s : %s", sender.getNickname(), msg);
        for (MyServer client : clients) {
            client.sendMsg(message);
        }
    }

    public static void sendToUser (MyServer from, String msg, String sendTo) throws IOException {
        String message = String.format("%s : %s", from.getNickname(), msg);
        for (MyServer client : clients) {
            if(client.getNickname().equals(sendTo)) {
                client.sendMsg("(personal) " + message);
                from.sendMsg("(personal) " + message);
                return;
            }
        }
        from.sendMsg("404 : User not found");
    }

    // метод для добавления нового клиента в коллекцию
    public static void addConnection (MyServer client) {
        clients.add(client);
    }

    // метод для удаления объекта из активных соединений
    public static void removeConnection (MyServer client) {
        clients.remove(client);
    }

    public static Authentication getAuth() {
        return auth;
    }

    // метод не дает зайти с одного аккаунта дважды
    public static boolean isLoggedIn (String nick) {
        for (MyServer client : clients) {
            if(client.getNickname().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public static void clientList() throws IOException {
        StringBuffer list = new StringBuffer();
        list.append("/client ");
        for (MyServer client : clients) {
            list.append(client.getNickname() + " ");
        }
        String msg = list.toString();
        for (MyServer client : clients) {
            client.sendMsg(msg);
        }
    }
}

class MyServer implements Runnable {
    Socket client;
    private String nickname;
    // вынос за scope для общей видимости всех методов
    DataInputStream in;
    DataOutputStream out;

    public MyServer(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(client.getInputStream());
            out = new DataOutputStream(client.getOutputStream());
            System.out.println("Client has connected");

            client.setSoTimeout(10000);

            // цикл аутентификации
            while(true) {
                String msg = in.readUTF();
                // проверяем протокол запроса аутентификации
                if(msg.startsWith("/auth")) {
                    //работаем с токеном
                    String [] token = msg.split("\\s");
                    String nick = Server.getAuth().getNickname(token[1], token[2]);

                    // проверяем на подлинность токена
                    if(nick != null && !Server.isLoggedIn(nick)) {
                        client.setSoTimeout(0);
                        nickname = nick;
                        // подтверждение (протокол ответа)
                        sendMsg("/authok " + nickname);
                        // добавление клиента в рабочую сеть
                        Server.addConnection(this);
                        Server.clientList();
                        System.out.println("Client " + nickname + " has connected to the network");
                        break;
                    } else {
                        sendMsg("/authno");
                    }
                }

                if(msg.startsWith("/reg")) {
                    String [] token = msg.split("\\s", 4);
                    if(token.length < 4) {
                        sendMsg("/400");
                    } else {
                        boolean isSignedUp = Server.getAuth().register(token[1], token[2], token[3]);
                        if(isSignedUp)
                            sendMsg("/200");
                        else
                            sendMsg("/400");
                        System.out.println(isSignedUp);
                    }
                }
            }

            // цикл работы
            while (true) {
                try {
                    String msg = in.readUTF();

                    if (msg.startsWith("/w")) {
                        // поле для отправки сообщения user
                        String [] token = msg.split("\\s", 3);
                        String username = token[1];
                        String str = token[2];
                        Server.sendToUser(this, str, username);
                    } else {
                        // поля для обычной отправки сообщений или выхода
                        if (msg.equals("/exit")) {
                            System.out.println("Client has disconnected");
                            sendMsg("/exit");
                            Server.removeConnection(this);
                            Server.clientList();
                            break;
                        }
                        Server.sendAll(this, msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (SocketTimeoutException e) {
            try {
                System.out.println("Client has been disconnected");
                client.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // метод для связи с sendAll
    public void sendMsg (String msg) throws IOException {
        out.writeUTF(msg);
    }

    public String getNickname() {
        return nickname;
    }
}

/* Внутренний протокол аутентификации
*  /auth login password (запрос)
*  /authok nickname (ответ)
*  /authno (ответ)
*
*  Внутренний протокол для отправки сообщения другому пользователю
*  /w nickname message (запрос)
*  /200 ok message (ответ)
*  /404 (ответ)
*
*  Протокол для списка активных клиентов
*  /client nick1,nick2...
*
*  Протокол регистрации
*  /reg login password name
*  /200 (ответ + )
*  /400 (ответ - )
* */