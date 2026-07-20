package framework.data.customer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Holds customer/test data for a single test.
 * Populated in {@code BaseTest} from a JSON file named after the test,
 * so tests read attributes from this object instead of parsing JSON themselves.
 * Unknown JSON attributes are ignored so one POJO can back many test data files.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerData {

    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String email;

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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
