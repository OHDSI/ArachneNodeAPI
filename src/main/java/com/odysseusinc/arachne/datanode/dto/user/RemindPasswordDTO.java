package com.odysseusinc.arachne.datanode.dto.user;


import org.hibernate.validator.constraints.Email;

import javax.validation.constraints.NotNull;

public class RemindPasswordDTO {

    @NotNull
    @Email
    private String email;

    public String getEmail() {

        return email;
    }

    public void setEmail(String email) {

        this.email = email;
    }
}