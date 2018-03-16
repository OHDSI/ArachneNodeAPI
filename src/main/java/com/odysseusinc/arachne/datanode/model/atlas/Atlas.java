package com.odysseusinc.arachne.datanode.model.atlas;

import com.odysseusinc.arachne.datanode.service.client.atlas.AtlasAuthSchema;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "atlases")
public class Atlas {

    @Id
    @SequenceGenerator(name = "atlases_pk_sequence", sequenceName = "atlases_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "atlases_pk_sequence")
    private Long id;

    @Column
    private Long centralId;

    @Column
    private String name;

    @Column
    private String url;

    @Column
    private String version;

    @Column
    @Enumerated(value = EnumType.STRING)
    private AtlasAuthSchema authType;

    @Column
    private String username;

    @Column
    @Type(type = "encryptedString")
    private String password;

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Atlas atlas = (Atlas) o;

        return getId() != null ? getId().equals(atlas.getId()) : atlas.getId() == null;
    }

    @Override
    public int hashCode() {

        return getId() != null ? getId().hashCode() : 0;
    }

    public Atlas() {

    }

    public Atlas(Long id) {

        setId(id);
    }

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public Long getCentralId() {

        return centralId;
    }

    public void setCentralId(Long centralId) {

        this.centralId = centralId;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getUrl() {

        return url;
    }

    public void setUrl(String url) {

        this.url = url;
    }

    public String getVersion() {

        return version;
    }

    public void setVersion(String version) {

        this.version = version;
    }

    public AtlasAuthSchema getAuthType() {

        return authType;
    }

    public void setAuthType(AtlasAuthSchema authType) {

        this.authType = authType;
    }

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
