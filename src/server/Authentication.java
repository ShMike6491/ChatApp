package server;

public interface Authentication {
    /**
     * @return nickname if exists, null if does not exist
     */
    public String getNickname(String login, String password);

    public boolean register (String login, String password, String nickname);
}
