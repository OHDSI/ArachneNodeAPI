/*
 *
 * Copyright 2019 Odysseus Data Services, inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Vitaly Koulakov, Anastasiia Klochkova, Sergej Suvorov, Anton Stepanov
 * Created: Jul 25, 2019
 *
 */

package com.odysseusinc.arachne.datanode.model.datanode;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "postponed_requests")
public class PostponedRequest {

    @Id
    @SequenceGenerator(name = "postponed_requests_id_seq", sequenceName = "postponed_requests_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "postponed_requests_id_seq")
    private Long id;

    @Column(name = "object_class")
    private String objectClass;

    @Column(name = "action")
    private String action;

    @Column(name = "args")
    private String args;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private PostponedRequestState state;

    @Column(name = "reason")
    private String reason;

    @Column(name = "last_send")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastSent;

    @Column(name = "created_at", updatable = false)
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "retries")
    private Integer retries;

    @Column(name = "username")
    private String username;

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public String getObjectClass() {

        return objectClass;
    }

    public void setObjectClass(String objectClass) {

        this.objectClass = objectClass;
    }

    public String getAction() {

        return action;
    }

    public void setAction(String action) {

        this.action = action;
    }

    public String getArgs() {

        return args;
    }

    public void setArgs(String args) {

        this.args = args;
    }

    public PostponedRequestState getState() {

        return state;
    }

    public void setState(PostponedRequestState state) {

        this.state = state;
    }

    public String getReason() {

        return reason;
    }

    public void setReason(String reason) {

        this.reason = reason;
    }

    public Date getLastSent() {

        return lastSent;
    }

    public void setLastSent(Date lastSent) {

        this.lastSent = lastSent;
    }

    public Integer getRetries() {

        return retries;
    }

    public void setRetries(Integer retries) {

        this.retries = retries;
    }

    public Date getCreatedAt() {

        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {

        this.createdAt = createdAt;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {

        this.username = username;
    }
}
