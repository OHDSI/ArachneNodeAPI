package com.odysseusinc.arachne.datanode.dto.serialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

public class MultiFormatDateDeserializer extends JsonDeserializer<Date> {

    private static final String[] DATE_FORMATS = {"yyyy-MM-dd, HH:mm", "yyyy-MM-dd HH:mm"};

    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {

        if (Objects.isNull(jsonParser) || StringUtils.isBlank(jsonParser.getText())) {
            return null;
        }
        String dateString = jsonParser.getText();
        for(String format : DATE_FORMATS) {
            try {
                return new SimpleDateFormat(format).parse(dateString);
            } catch (ParseException ignored) {
            }
        }
        throw new InvalidFormatException(jsonParser, "Cannot deserialize value", dateString, Date.class);
    }
}
