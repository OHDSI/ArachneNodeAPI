package com.odysseusinc.arachne.datanode.model.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.Embeddable;

/**
 * @author vkoulakov
 * @since 4/24/17.
 */
@Embeddable
public class AnalysisAuthor {
    private String firstName;
    private String lastName;
    private String email;

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

    @JsonProperty(value = "fullName")
    public String fullName() {

        String result = "";
        if (firstName != null) {
            result += firstName;
        }
        if (lastName != null) {
            if (firstName != null) {
                result += " ";
            }
            result += lastName;
        } else if (firstName == null) {
            result = email;
        }
        return result;
    }
}
