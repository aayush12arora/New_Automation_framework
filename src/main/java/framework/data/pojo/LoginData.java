package framework.data.pojo;

/**
 * Simple POJO representing login test data.
 * Populated from JSON via {@link framework.utilities.JsonUtils}.
 */
public class LoginData {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
