package com.odysseusinc.arachne.datanode.dto.atlas;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.odysseusinc.arachne.datanode.dto.serialize.MultiFormatDateDeserializer;
import java.util.Date;

public class CohortCharacterization extends BaseAtlasEntity {

	private Long id;
	private AtlasUserDTO createdBy;
	@JsonDeserialize(using = MultiFormatDateDeserializer.class)
	private Date createdAt;
	private AtlasUserDTO updatedBy;
	@JsonDeserialize(using = MultiFormatDateDeserializer.class)
	private Date updatedAt;
	private Integer hashCode;
	private String name;

	public Long getId() {

		return id;
	}

	public void setId(Long id) {

		this.id = id;
	}

	public AtlasUserDTO getCreatedBy() {

		return createdBy;
	}

	public void setCreatedBy(AtlasUserDTO createdBy) {

		this.createdBy = createdBy;
	}

	public Date getCreatedAt() {

		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {

		this.createdAt = createdAt;
	}

	public AtlasUserDTO getUpdatedBy() {

		return updatedBy;
	}

	public void setUpdatedBy(AtlasUserDTO updatedBy) {

		this.updatedBy = updatedBy;
	}

	public Date getUpdatedAt() {

		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {

		this.updatedAt = updatedAt;
	}

	public Integer getHashCode() {

		return hashCode;
	}

	public void setHashCode(Integer hashCode) {

		this.hashCode = hashCode;
	}

	@Override
	public String getName() {

		return name;
	}

	@Override
	public void setName(String name) {

		this.name = name;
	}
}
