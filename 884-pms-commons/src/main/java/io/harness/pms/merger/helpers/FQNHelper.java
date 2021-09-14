/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.merger.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.yaml.YAMLFieldNameConstants;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class FQNHelper {
  List<String> possibleUUIDs = Arrays.asList(YAMLFieldNameConstants.IDENTIFIER, YAMLFieldNameConstants.NAME,
      YAMLFieldNameConstants.KEY, YAMLFieldNameConstants.COMMAND_TYPE);

  public void validateUniqueFqn(FQN fqn, Object value, Map<FQN, Object> res, HashSet<String> expressions) {
    String expressionFqn = fqn.getExpressionFqn();
    if (expressions.contains(expressionFqn)) {
      String fqnDisplay = fqn.display();
      throw new InvalidRequestException(String.format(" This element is coming twice in yaml %s",
          fqn.display().substring(0, fqnDisplay.lastIndexOf('.', fqnDisplay.length() - 2))));
    }
    expressions.add(expressionFqn);
    res.put(fqn, value);
  }

  public boolean checkIfListHasNoIdentifier(ArrayNode list) {
    JsonNode firstNode = list.get(0);
    Set<String> fieldNames = new LinkedHashSet<>();
    firstNode.fieldNames().forEachRemaining(fieldNames::add);
    String topKey = fieldNames.iterator().next();
    if (topKey.equals(YAMLFieldNameConstants.PARALLEL)) {
      return false;
    }
    JsonNode innerMap = firstNode.get(topKey);
    return !innerMap.has(YAMLFieldNameConstants.IDENTIFIER);
  }

  public String getUuidKey(ArrayNode list) {
    JsonNode element = list.get(0);
    for (String uuidKey : possibleUUIDs) {
      if (element.has(uuidKey)) {
        return uuidKey;
      }
    }
    return "";
  }
}
