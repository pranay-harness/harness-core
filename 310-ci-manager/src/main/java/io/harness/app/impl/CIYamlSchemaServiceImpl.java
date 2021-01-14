package io.harness.app.impl;

import static io.harness.yaml.schema.beans.SchemaConstants.DEFINITIONS_NODE;

import io.harness.EntityType;
import io.harness.app.intfc.CIYamlSchemaService;
import io.harness.encryption.Scope;
import io.harness.jackson.JsonNodeUtils;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.yaml.schema.SchemaGeneratorUtils;
import io.harness.yaml.schema.YamlSchemaGenerator;
import io.harness.yaml.schema.YamlSchemaProvider;
import io.harness.yaml.schema.beans.FieldSubtypeData;
import io.harness.yaml.schema.beans.SubtypeClassMap;
import io.harness.yaml.schema.beans.SwaggerDefinitionsMetaInfo;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class CIYamlSchemaServiceImpl implements CIYamlSchemaService {
  public static final String STEP_ELEMENT_CONFIG = StepElementConfig.class.getSimpleName();
  public static final Class<StepElementConfig> STEP_ELEMENT_CONFIG_CLASS = StepElementConfig.class;
  private final YamlSchemaProvider yamlSchemaProvider;
  private final YamlSchemaGenerator yamlSchemaGenerator;

  @Override
  public JsonNode getIntegrationStageYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope) {
    JsonNode integrationStageSchema =
        yamlSchemaProvider.getYamlSchema(EntityType.INTEGRATION_STAGE, orgIdentifier, projectIdentifier, scope);
    JsonNode integrationStageSteps =
        yamlSchemaProvider.getYamlSchema(EntityType.INTEGRATION_STEPS, orgIdentifier, projectIdentifier, scope);

    JsonNode definitions = integrationStageSchema.get(DEFINITIONS_NODE);
    JsonNode stepDefinitions = integrationStageSteps.get(DEFINITIONS_NODE);
    JsonNode mergedDefinitions = JsonNodeUtils.merge(definitions, stepDefinitions);

    JsonNode jsonNode = mergedDefinitions.get(StepElementConfig.class.getSimpleName());
    modifyStepElementSchema((ObjectNode) jsonNode);

    return ((ObjectNode) integrationStageSchema).set(DEFINITIONS_NODE, mergedDefinitions);
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
}
