package com.odysseusinc.arachne.datanode.dto.atlas;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.odysseusinc.arachne.datanode.dto.serialize.MultiFormatDateDeserializer;
import java.util.Date;
import java.util.List;

public class Pathway extends BaseAtlasEntity {

    private Long id;
    private String name;
    private List<PathwayCohort> targetCohorts;
    private List<PathwayCohort> eventCohorts;
    private Integer combinationWindow;
    private Integer minCellCount;
    private Integer maxDepth;
    private boolean allowRepeats;
    private String createdBy;
    @JsonDeserialize(using = MultiFormatDateDeserializer.class)
    private Date createdDate;
    private String modifiedBy;
    @JsonDeserialize(using = MultiFormatDateDeserializer.class)
    private Date modifiedDate;
    private Integer hashCode;

    public Long getId() {

        return id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public List<PathwayCohort> getTargetCohorts() {

        return targetCohorts;
    }

    public void setTargetCohorts(List<PathwayCohort> targetCohorts) {

        this.targetCohorts = targetCohorts;
    }

    public List<PathwayCohort> getEventCohorts() {

        return eventCohorts;
    }

    public void setEventCohorts(List<PathwayCohort> eventCohorts) {

        this.eventCohorts = eventCohorts;
    }

    public Integer getCombinationWindow() {

        return combinationWindow;
    }

    public void setCombinationWindow(Integer combinationWindow) {

        this.combinationWindow = combinationWindow;
    }

    public Integer getMinCellCount() {

        return minCellCount;
    }

    public void setMinCellCount(Integer minCellCount) {

        this.minCellCount = minCellCount;
    }

    public Integer getMaxDepth() {

        return maxDepth;
    }

    public void setMaxDepth(Integer maxDepth) {

        this.maxDepth = maxDepth;
    }

    public boolean isAllowRepeats() {

        return allowRepeats;
    }

    public void setAllowRepeats(boolean allowRepeats) {

        this.allowRepeats = allowRepeats;
    }

    public String getCreatedBy() {

        return createdBy;
    }

    public void setCreatedBy(String createdBy) {

        this.createdBy = createdBy;
    }

    public Date getCreatedDate() {

        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {

        this.createdDate = createdDate;
    }

    public String getModifiedBy() {

        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {

        this.modifiedBy = modifiedBy;
    }

    public Date getModifiedDate() {

        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {

        this.modifiedDate = modifiedDate;
    }

    public Integer getHashCode() {

        return hashCode;
    }

    public void setHashCode(Integer hashCode) {

        this.hashCode = hashCode;
    }
}
