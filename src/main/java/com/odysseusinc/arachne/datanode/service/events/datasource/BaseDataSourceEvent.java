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
 * Created: Jul 29, 2019
 *
 */

package com.odysseusinc.arachne.datanode.service.events.datasource;

import com.odysseusinc.arachne.datanode.model.datasource.DataSource;
import com.odysseusinc.arachne.datanode.model.user.User;
import org.springframework.context.ApplicationEvent;

public abstract class BaseDataSourceEvent extends ApplicationEvent {
    private User owner;
    private DataSource dataSource;

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public BaseDataSourceEvent(Object source, User owner, DataSource dataSource) {

        super(source);
        this.owner = owner;
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {

        return dataSource;
    }

    public User getOwner() {

        return owner;
    }
}
