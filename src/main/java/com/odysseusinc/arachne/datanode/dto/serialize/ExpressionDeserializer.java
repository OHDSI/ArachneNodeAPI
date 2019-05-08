package com.odysseusinc.arachne.datanode.dto.serialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.Objects;

/**
 *  In older Atlas versions Cohort expression was a String containing json inside,
 *  but starting from version 2.8 it becomes an Object.
 *  ExpressionDeserializer is required to remain consistent w/o changing internal logic of DataNode.
 */
public class ExpressionDeserializer extends JsonDeserializer<String> {
	@Override
	public String deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {

		if (Objects.isNull(parser)) {
			return null;
		}
		JsonToken token = parser.getCurrentToken();
		if (Objects.equals(token, JsonToken.VALUE_STRING)) {
			return parser.getValueAsString();
		}
		TreeNode treeNode = parser.getCodec().readTree(parser);

		return treeNode.toString();
	}
}
