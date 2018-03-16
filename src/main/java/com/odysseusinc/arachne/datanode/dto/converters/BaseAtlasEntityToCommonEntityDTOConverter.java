package com.odysseusinc.arachne.datanode.dto.converters;

import com.odysseusinc.arachne.commons.api.v1.dto.CommonEntityDTO;
import com.odysseusinc.arachne.datanode.dto.atlas.BaseAtlasEntity;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;

public abstract class BaseAtlasEntityToCommonEntityDTOConverter<S extends BaseAtlasEntity, T extends CommonEntityDTO>
        implements Converter<S, T>, InitializingBean {

    protected GenericConversionService conversionService;

    public BaseAtlasEntityToCommonEntityDTOConverter(GenericConversionService conversionService) {

        this.conversionService = conversionService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        conversionService.addConverter(this);
    }

    @Override
    public T convert(S source) {

        T result = getTargetClass();
        result.setOriginId(source.getOrigin().getId());
        result.setName(source.getName());
        return result;
    }

    protected abstract T getTargetClass();
}
