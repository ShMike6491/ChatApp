package server;

import java.util.ArrayList;
import java.util.List;

public class UsersAuth implements Authentication{
    private class User {
        String login;
        String password;
        String nickname;

        public User(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
    }

    List<User> users;

    public UsersAuth() {
        users = new ArrayList<>();

        users.add(new User("qwe", "qwe", "qwe"));
        users.add(new User("asd", "asd", "asd"));
        users.add(new User("zxc", "zxc", "zxc"));
    }

    @Override
    public String getNickname(String login, String password) {
        for (User user:users) {
            if(user.login.equals(login) && user.password.equals(password)) {
                return user.nickname;
            }
        }
        return null;
    }

    @Override
    public boolean register(String login, String password, String nickname) {
        for (User user:users ) {
            if(user.login.equals(login) || user.nickname.equals(nickname))
                return false;
        }
        users.add(new User(login, password, nickname));
        return true;
    }
}
