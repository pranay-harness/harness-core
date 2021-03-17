package io.harness.app.impl;

import static io.harness.yaml.schema.beans.SchemaConstants.DEFINITIONS_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.PROPERTIES_NODE;

import io.harness.EntityType;
import io.harness.app.intfc.CIYamlSchemaService;
import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.encryption.Scope;
import io.harness.jackson.JsonNodeUtils;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.yaml.schema.SchemaGeneratorUtils;
import io.harness.yaml.schema.YamlSchemaGenerator;
import io.harness.yaml.schema.YamlSchemaProvider;
import io.harness.yaml.schema.beans.FieldSubtypeData;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.SchemaConstants;
import io.harness.yaml.schema.beans.SubtypeClassMap;
import io.harness.yaml.schema.beans.SwaggerDefinitionsMetaInfo;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class CIYamlSchemaServiceImpl implements CIYamlSchemaService {
  private static final String INTEGRATION_STAGE_CONFIG = YamlSchemaUtils.getSwaggerName(IntegrationStageConfig.class);
  private static final String STEP_ELEMENT_CONFIG = YamlSchemaUtils.getSwaggerName(StepElementConfig.class);
  private static final Class<StepElementConfig> STEP_ELEMENT_CONFIG_CLASS = StepElementConfig.class;
  private static final String CI_NAMESPACE = "ci";
  private final YamlSchemaProvider yamlSchemaProvider;
  private final YamlSchemaGenerator yamlSchemaGenerator;

  @Override
  public PartialSchemaDTO getIntegrationStageYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope) {
    JsonNode integrationStageSchema =
        yamlSchemaProvider.getYamlSchema(EntityType.INTEGRATION_STAGE, orgIdentifier, projectIdentifier, scope);
    JsonNode integrationStageSteps =
        yamlSchemaProvider.getYamlSchema(EntityType.INTEGRATION_STEPS, orgIdentifier, projectIdentifier, scope);
    JsonNode pipelineStepsSchema =
        yamlSchemaProvider.getYamlSchema(EntityType.PIPELINE_STEPS, orgIdentifier, projectIdentifier, scope);

    JsonNode definitions = integrationStageSchema.get(DEFINITIONS_NODE);
    JsonNode integrationStepDefinitions = integrationStageSteps.get(DEFINITIONS_NODE);
    JsonNode pipelineStepDefinitions = pipelineStepsSchema.get(DEFINITIONS_NODE);

    JsonNode stepDefinitions = JsonNodeUtils.merge(integrationStepDefinitions, pipelineStepDefinitions);
    JsonNode mergedDefinitions = JsonNodeUtils.merge(definitions, stepDefinitions);

    JsonNode jsonNode = mergedDefinitions.get(StepElementConfig.class.getSimpleName());
    modifyStepElementSchema((ObjectNode) jsonNode);

    jsonNode = mergedDefinitions.get(ParallelStepElementConfig.class.getSimpleName());
    if (jsonNode.isObject()) {
      flattenParallelStepElementConfig((ObjectNode) jsonNode);
    }
    removeUnwantedNodes(mergedDefinitions);

    yamlSchemaGenerator.modifyRefsNamespace(integrationStageSchema, CI_NAMESPACE);
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    JsonNode node = mapper.createObjectNode().set(CI_NAMESPACE, mergedDefinitions);

    JsonNode partialCiSchema = ((ObjectNode) integrationStageSchema).set(DEFINITIONS_NODE, node);

    return PartialSchemaDTO.builder()
        .namespace(CI_NAMESPACE)
        .nodeName(INTEGRATION_STAGE_CONFIG)
        .schema(partialCiSchema)
        .nodeType("CI")
        .build();
  }

  private void modifyStepElementSchema(ObjectNode jsonNode) {
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap = new HashMap<>();
    Field typedField = YamlSchemaUtils.getTypedField(STEP_ELEMENT_CONFIG_CLASS);
    Set<SubtypeClassMap> mapOfSubtypes = YamlSchemaUtils.getMapOfSubtypesUsingReflection(typedField);
    Set<FieldSubtypeData> classFieldSubtypeData = new HashSet<>();
    classFieldSubtypeData.add(YamlSchemaUtils.getFieldSubtypeData(typedField, mapOfSubtypes));
    swaggerDefinitionsMetaInfoMap.put(
        STEP_ELEMENT_CONFIG, SwaggerDefinitionsMetaInfo.builder().subtypeClassMap(classFieldSubtypeData).build());
    yamlSchemaGenerator.convertSwaggerToJsonSchema(
        swaggerDefinitionsMetaInfoMap, mapper, STEP_ELEMENT_CONFIG, jsonNode);
  }

  private void flattenParallelStepElementConfig(ObjectNode objectNode) {
    JsonNode sections = objectNode.get(PROPERTIES_NODE).get("sections");
    if (sections.isObject()) {
      objectNode.removeAll();
      objectNode.setAll((ObjectNode) sections);
      objectNode.put(SchemaConstants.SCHEMA_NODE, SchemaConstants.JSON_SCHEMA_7);
    }
  }

  private void removeUnwantedNodes(JsonNode definitions) {
    if (definitions.isObject()) {
      Iterator<JsonNode> elements = definitions.elements();
      while (elements.hasNext()) {
        JsonNode jsonNode = elements.next();
        yamlSchemaGenerator.removeUnwantedNodes(jsonNode, "rollbackSteps");
        yamlSchemaGenerator.removeUnwantedNodes(jsonNode, "failureStrategies");
        yamlSchemaGenerator.removeUnwantedNodes(jsonNode, "stepGroup");
      }
    }
  }

  private String getIntegrationStageTypeName() {
    JsonTypeName annotation = IntegrationStageConfig.class.getAnnotation(JsonTypeName.class);
    return annotation.value();
  }
}
