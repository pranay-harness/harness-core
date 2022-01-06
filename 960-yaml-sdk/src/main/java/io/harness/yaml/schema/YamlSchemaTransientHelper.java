/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.schema;

import static io.harness.yaml.schema.beans.SchemaConstants.ALL_OF_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.CONST_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.ENUM_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.IF_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.PROPERTIES_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.TYPE_NODE;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.jackson.JsonNodeUtils;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
// Todo: to be deleted after finishing the steps migration to new schema
public class YamlSchemaTransientHelper {
  // Add all steps in this list that are moved to new schema.
  public static final List<EntityType> allStepV2EntityTypes = new ArrayList<EntityType>() {
    {
      add(EntityType.HTTP_STEP);
      add(EntityType.JIRA_CREATE_STEP);
      add(EntityType.SHELL_SCRIPT_STEP);
      add(EntityType.K8S_CANARY_DEPLOY_STEP);
      add(EntityType.TEMPLATE);
      add(EntityType.SERVICENOW_APPROVAL_STEP);
    }
  };

  public void deleteSpecNodeInStageElementConfig(JsonNode stageElementConfig) {
    JsonNodeUtils.deletePropertiesInJsonNode((ObjectNode) stageElementConfig.get(PROPERTIES_NODE), "spec");
  }

  // Removing the step nodes from type enum that are migrated to new schema
  public Set<Class<?>> removeNewSchemaStepsSubtypes(Set<Class<?>> subTypes, Collection<Class<?>> newStepsToBeRemoved) {
    Set<Class<?>> newSchemaSteps = new HashSet<>();
    for (Class<?> clazz : newStepsToBeRemoved) {
      if (YamlSchemaUtils.getTypedField(clazz) != null) {
        newSchemaSteps.add(YamlSchemaUtils.getTypedField(clazz).getType());
      }
    }
    return subTypes.stream().filter(o -> !newSchemaSteps.contains(o)).collect(Collectors.toSet());
  }

  public void removeV2StepEnumsFromStepElementConfig(JsonNode stepElementConfigNode) {
    Set<String> v2StepTypes = allStepV2EntityTypes.stream().map(EntityType::getYamlName).collect(Collectors.toSet());
    removeV2StepFromStepElementConfigAllOf((ArrayNode) stepElementConfigNode.get(ALL_OF_NODE), v2StepTypes);
    ArrayNode enumNode = (ArrayNode) stepElementConfigNode.get(PROPERTIES_NODE).get(TYPE_NODE).get(ENUM_NODE);
    if (enumNode == null) {
      return;
    }
    ArrayNode enumArray = enumNode.deepCopy();
    enumNode.removeAll();
    for (JsonNode arrayElement : enumArray) {
      if (!v2StepTypes.contains(arrayElement.asText())) {
        enumNode.add(arrayElement);
      }
    }
  }
  public void removeV2StepFromStepElementConfigAllOf(ArrayNode allOfNode, Set<String> v2StepTypes) {
    if (allOfNode == null) {
      return;
    }
    ArrayNode allOfArray = allOfNode.deepCopy();
    allOfNode.removeAll();
    for (JsonNode arrayElement : allOfArray) {
      if (!v2StepTypes.contains(
              arrayElement.get(IF_NODE).get(PROPERTIES_NODE).get(TYPE_NODE).get(CONST_NODE).asText())) {
        allOfNode.add(arrayElement);
      }
    }
  }
}
