package server;

public interface Authentication {
    /**
     * @return nickname if exists, null if does not exist
     */
    public String getNickname(String login, String password);
}
