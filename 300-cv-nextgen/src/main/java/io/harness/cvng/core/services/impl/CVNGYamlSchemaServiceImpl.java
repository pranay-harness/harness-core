package io.harness.cvng.core.services.impl;

import static io.harness.yaml.schema.beans.SchemaConstants.DEFINITIONS_NODE;

import io.harness.EntityType;
import io.harness.cvng.cdng.beans.CVNGStepInfo;
import io.harness.cvng.core.services.api.CVNGYamlSchemaService;
import io.harness.encryption.Scope;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.yaml.schema.SchemaGeneratorUtils;
import io.harness.yaml.schema.YamlSchemaGenerator;
import io.harness.yaml.schema.YamlSchemaProvider;
import io.harness.yaml.schema.beans.FieldEnumData;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.SubtypeClassMap;
import io.harness.yaml.schema.beans.SwaggerDefinitionsMetaInfo;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class CVNGYamlSchemaServiceImpl implements CVNGYamlSchemaService {
  private static final String CVNG_NAMESPACE = "cvng";
  private static final String STEP_ELEMENT_CONFIG = YamlSchemaUtils.getSwaggerName(StepElementConfig.class);
  private static final Class<StepElementConfig> STEP_ELEMENT_CONFIG_CLASS = StepElementConfig.class;

  private final YamlSchemaProvider yamlSchemaProvider;
  private final YamlSchemaGenerator yamlSchemaGenerator;

  @Override
  public PartialSchemaDTO getDeploymentStageYamlSchema(String orgIdentifier, String projectIdentifier, Scope scope) {
    JsonNode deploymentSteps =
        yamlSchemaProvider.getYamlSchema(EntityType.DEPLOYMENT_STEPS, orgIdentifier, projectIdentifier, scope);
    JsonNode definitions = deploymentSteps.get(DEFINITIONS_NODE);

    JsonNode stepElementConfigNode = definitions.get(StepElementConfig.class.getSimpleName());
    if (stepElementConfigNode != null && stepElementConfigNode.isObject()) {
      modifyStepElementSchema((ObjectNode) stepElementConfigNode);
    }

    yamlSchemaGenerator.modifyRefsNamespace(deploymentSteps, CVNG_NAMESPACE);
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    JsonNode node = mapper.createObjectNode().set(CVNG_NAMESPACE, definitions);
    JsonNode partialCVNGSchema = ((ObjectNode) deploymentSteps).set(DEFINITIONS_NODE, node);
    return PartialSchemaDTO.builder()
        .namespace(CVNG_NAMESPACE)
        .nodeName(YamlSchemaUtils.getSwaggerName(CVNGStepInfo.class))
        .schema(partialCVNGSchema)
        .nodeType(getVerifyStepTypeName())
        .build();
  }

  private void modifyStepElementSchema(ObjectNode jsonNode) {
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap = new HashMap<>();

    Set<FieldEnumData> fieldEnumData = getFieldEnumData(STEP_ELEMENT_CONFIG_CLASS);

    swaggerDefinitionsMetaInfoMap.put(
        STEP_ELEMENT_CONFIG, SwaggerDefinitionsMetaInfo.builder().fieldEnumData(fieldEnumData).build());

    yamlSchemaGenerator.convertSwaggerToJsonSchema(
        swaggerDefinitionsMetaInfoMap, mapper, STEP_ELEMENT_CONFIG, jsonNode);
  }

  private Set<FieldEnumData> getFieldEnumData(Class<?> clazz) {
    Field typedField = YamlSchemaUtils.getTypedField(clazz);
    String fieldName = YamlSchemaUtils.getJsonTypeInfo(typedField).property();
    Set<SubtypeClassMap> mapOfSubtypes = YamlSchemaUtils.getMapOfSubtypesUsingReflection(typedField);

    return ImmutableSet.of(
        FieldEnumData.builder()
            .fieldName(fieldName)
            .enumValues(ImmutableSortedSet.copyOf(
                mapOfSubtypes.stream().map(SubtypeClassMap::getSubtypeEnum).collect(Collectors.toList())))
            .build());
  }

  private String getVerifyStepTypeName() {
    JsonTypeName annotation = CVNGStepInfo.class.getAnnotation(JsonTypeName.class);
    return annotation.value();
  }
}
