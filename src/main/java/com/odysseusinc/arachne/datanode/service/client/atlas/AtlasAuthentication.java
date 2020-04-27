package com.odysseusinc.arachne.datanode.service.client.atlas;

public class AtlasAuthentication {
    private AtlasAuthSchema schema;
    private String username;
    private String password;
    private String keyfile;
    private String serviceId;

    public void setSchema(AtlasAuthSchema schema) {
        this.schema = schema;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setKeyfile(String keyfile) {
        this.keyfile = keyfile;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public AtlasAuthSchema getSchema() {
        return schema;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getKeyfile() {
        return keyfile;
    }

    public String getServiceId() {
        return serviceId;
    }
}
