package com.odysseusinc.arachne.datanode.model.datasource;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonModelType;
import com.odysseusinc.arachne.commons.types.CommonCDMVersionDTO;

public class AutoDetectedFields {

    private CommonCDMVersionDTO cdmVersion;
    private CommonModelType commonModelType;

    public AutoDetectedFields() {
    }

    public AutoDetectedFields(CommonModelType commonModelType) {
        this.commonModelType = commonModelType;
    }

    public AutoDetectedFields(CommonModelType commonModelType, CommonCDMVersionDTO cdmVersion) {
        this.cdmVersion = cdmVersion;
        this.commonModelType = commonModelType;
    }

    public CommonCDMVersionDTO getCdmVersion() {
        return cdmVersion;
    }

    public void setCdmVersion(CommonCDMVersionDTO cdmVersion) {
        this.cdmVersion = cdmVersion;
    }

    public CommonModelType getCommonModelType() {
        return commonModelType;
    }

    public void setCommonModelType(CommonModelType commonModelType) {
        this.commonModelType = commonModelType;
    }
}
