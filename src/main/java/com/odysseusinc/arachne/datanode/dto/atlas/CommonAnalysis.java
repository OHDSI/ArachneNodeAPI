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
 * Created: Apr 26, 2019
 *
 */

package com.odysseusinc.arachne.datanode.dto.atlas;

import java.util.Date;

public abstract class CommonAnalysis extends BaseAtlasEntity {
	private String name;
	private String description;
	//TODO add custom deserializer to resolve case with insecure Atlas returning empty string as createdBy
//	private AtlasUserDTO createdBy;
	private Date createdDate;
//	private AtlasUserDTO modifiedBy;
	private Date modifiedDate;

	public abstract Integer getId();

	@Override
	public String getName() {

		return name;
	}

	@Override
	public void setName(String name) {

		this.name = name;
	}

	public String getDescription() {

		return description;
	}

	public void setDescription(String description) {

		this.description = description;
	}

/*
	public AtlasUserDTO getCreatedBy() {

		return createdBy;
	}

	public void setCreatedBy(AtlasUserDTO createdBy) {

		this.createdBy = createdBy;
	}
*/

	public Date getCreatedDate() {

		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {

		this.createdDate = createdDate;
	}

/*
	public AtlasUserDTO getModifiedBy() {

		return modifiedBy;
	}

	public void setModifiedBy(AtlasUserDTO modifiedBy) {

		this.modifiedBy = modifiedBy;
	}
*/

	public Date getModifiedDate() {

		return modifiedDate;
	}

	public void setModifiedDate(Date modifiedDate) {

		this.modifiedDate = modifiedDate;
	}
}
