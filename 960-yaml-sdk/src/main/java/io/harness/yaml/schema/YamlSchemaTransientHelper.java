package io.harness.yaml.schema;

import static io.harness.yaml.schema.beans.SchemaConstants.ENUM_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.ONE_OF_NODE;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
// Todo: to be deleted after finishing the steps migration to new schema
public class YamlSchemaTransientHelper {
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
    for (JsonNode oneOfElement : stepElementConfigNode.get(ONE_OF_NODE)) {
      if (oneOfElement.get(PROPERTIES_NODE).get(TYPE_NODE) == null) {
        continue;
      }
      ArrayNode enumNode = (ArrayNode) oneOfElement.get(PROPERTIES_NODE).get(TYPE_NODE).get(ENUM_NODE);
      ArrayNode enumArray = enumNode.deepCopy();
      enumNode.removeAll();
      for (JsonNode arrayElement : enumArray) {
        // TODO: Use V2-steps-entity-type-list that is being added in shell script movement PR.
        if (!arrayElement.asText().equals(EntityType.HTTP_STEP.getYamlName())) {
          enumNode.add(arrayElement);
        }
      }
    }
  }
}
