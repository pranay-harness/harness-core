/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.filter.utils;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Criteria;

@UtilityClass
public class ModuleInfoFilterUtils {
  public void processNode(JsonNode jsonNode, String parentPath, Criteria criteria) {
    if (jsonNode.isValueNode()) {
      criteria.and(parentPath).is(jsonNode.asText());
    } else if (jsonNode.isArray()) {
      List<String> valueList = new ArrayList<>();
      for (JsonNode arrayItem : jsonNode) {
        valueList.add(arrayItem.textValue());
      }
      criteria.and(parentPath).in(valueList);
    } else if (jsonNode.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> jsonField = fields.next();
        processNode(jsonField.getValue(), String.join(".", parentPath, jsonField.getKey()), criteria);
      }
    }
  }
}
