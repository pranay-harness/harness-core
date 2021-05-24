package io.harness.pms.pipeline.service;

import static io.harness.yaml.schema.beans.SchemaConstants.DEFINITIONS_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.PROPERTIES_NODE;

import static java.lang.String.format;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;
import io.harness.exception.JsonSchemaValidationException;
import io.harness.jackson.JsonNodeUtils;
import io.harness.network.SafeHttpCall;
import io.harness.plancreator.stages.parallel.ParallelStageElementConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.utils.PmsConstants;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.approval.stage.ApprovalStageConfig;
import io.harness.yaml.schema.SchemaGeneratorUtils;
import io.harness.yaml.schema.YamlSchemaGenerator;
import io.harness.yaml.schema.YamlSchemaProvider;
import io.harness.yaml.schema.beans.FieldEnumData;
import io.harness.yaml.schema.beans.FieldSubtypeData;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.SchemaConstants;
import io.harness.yaml.schema.beans.SubtypeClassMap;
import io.harness.yaml.schema.beans.SwaggerDefinitionsMetaInfo;
import io.harness.yaml.schema.client.YamlSchemaClient;
import io.harness.yaml.utils.JsonPipelineUtils;
import io.harness.yaml.utils.YamlSchemaUtils;
import io.harness.yaml.validator.YamlSchemaValidator;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class PMSYamlSchemaServiceImpl implements PMSYamlSchemaService {
  public static final String STAGE_ELEMENT_CONFIG = YamlSchemaUtils.getSwaggerName(StageElementConfig.class);
  public static final Class<StageElementConfig> STAGE_ELEMENT_CONFIG_CLASS = StageElementConfig.class;

  private static final String APPROVAL_STAGE_CONFIG = YamlSchemaUtils.getSwaggerName(ApprovalStageConfig.class);
  private static final String STEP_ELEMENT_CONFIG = YamlSchemaUtils.getSwaggerName(StepElementConfig.class);
  private static final Class<StepElementConfig> STEP_ELEMENT_CONFIG_CLASS = StepElementConfig.class;
  private static final String APPROVAL_NAMESPACE = "approval";

  private static final int CACHE_EVICTION_TIME_HOUR = 1;

  private final YamlSchemaProvider yamlSchemaProvider;
  private final YamlSchemaGenerator yamlSchemaGenerator;
  private final Map<String, YamlSchemaClient> yamlSchemaClientMapper;
  private final YamlSchemaValidator yamlSchemaValidator;

  private final PmsSdkInstanceService pmsSdkInstanceService;

  private final LoadingCache<SchemaKey, JsonNode> schemaCache =
      CacheBuilder.newBuilder()
          .expireAfterAccess(CACHE_EVICTION_TIME_HOUR, TimeUnit.HOURS)
          .build(new CacheLoader<SchemaKey, JsonNode>() {
            @Override
            public JsonNode load(@NotNull final SchemaKey schemaKey) {
              return getPipelineYamlSchema(schemaKey.getProjectId(), schemaKey.getOrgId(), schemaKey.getScope());
            }
          });

  public JsonNode getPipelineYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope) {
    JsonNode pipelineSchema =
        yamlSchemaProvider.getYamlSchema(EntityType.PIPELINES, orgIdentifier, projectIdentifier, scope);

    JsonNode pipelineSteps =
        yamlSchemaProvider.getYamlSchema(EntityType.PIPELINE_STEPS, orgIdentifier, projectIdentifier, scope);
    ObjectNode pipelineDefinitions = (ObjectNode) pipelineSchema.get(DEFINITIONS_NODE);
    ObjectNode pipelineStepsDefinitions = (ObjectNode) pipelineSteps.get(DEFINITIONS_NODE);

    ObjectNode mergedDefinitions = (ObjectNode) JsonNodeUtils.merge(pipelineDefinitions, pipelineStepsDefinitions);

    ObjectNode stageElementConfig = (ObjectNode) pipelineDefinitions.get(STAGE_ELEMENT_CONFIG);

    flattenParallelElementConfig(pipelineDefinitions);

    Set<String> instanceNames = pmsSdkInstanceService.getInstanceNames();
    instanceNames.remove(PmsConstants.INTERNAL_SERVICE_NAME);
    for (String instanceName : instanceNames) {
      PartialSchemaDTO partialSchemaDTO = getStage(instanceName, projectIdentifier, orgIdentifier, scope);
      if (partialSchemaDTO == null) {
        continue;
      }
      processPartialStageSchema(mergedDefinitions, pipelineStepsDefinitions, stageElementConfig, partialSchemaDTO);
    }
    processPartialStageSchema(mergedDefinitions, pipelineStepsDefinitions, stageElementConfig,
        getApprovalStage(projectIdentifier, orgIdentifier, scope));

    return ((ObjectNode) pipelineSchema).set(DEFINITIONS_NODE, pipelineDefinitions);
  }

  private void flattenParallelElementConfig(ObjectNode definitions) {
    // flatten stage
    JsonNode jsonNode = definitions.get(ParallelStageElementConfig.class.getSimpleName());
    if (jsonNode.isObject()) {
      flatten((ObjectNode) jsonNode);
    }

    // flatten step
    jsonNode = definitions.get(ParallelStepElementConfig.class.getSimpleName());
    if (jsonNode.isObject()) {
      flatten((ObjectNode) jsonNode);
    }
  }

  private void processPartialStageSchema(ObjectNode pipelineDefinitions, ObjectNode pipelineSteps,
      ObjectNode stageElementConfig, PartialSchemaDTO partialSchemaDTO) {
    SubtypeClassMap subtypeClassMap =
        SubtypeClassMap.builder()
            .subTypeDefinitionKey(partialSchemaDTO.getNamespace() + "/" + partialSchemaDTO.getNodeName())
            .subtypeEnum(partialSchemaDTO.getNodeType())
            .build();

    ObjectNode stageDefinitionsNode = moveRootNodeToDefinitions(
        partialSchemaDTO.getNodeName(), (ObjectNode) partialSchemaDTO.getSchema(), partialSchemaDTO.getNamespace());

    mergePipelineStepsIntoStage(stageDefinitionsNode, pipelineSteps, partialSchemaDTO);
    mergeStageElementConfig(stageElementConfig, subtypeClassMap);

    pipelineDefinitions.set(partialSchemaDTO.getNamespace(), stageDefinitionsNode.get(partialSchemaDTO.getNamespace()));
  }

  private void mergePipelineStepsIntoStage(
      ObjectNode stageDefinitionsNode, ObjectNode pipelineSteps, PartialSchemaDTO partialSchemaDTO) {
    ObjectNode pipelineStepsCopy = obtainPipelineStepsCopyWithCorrectNamespace(pipelineSteps, partialSchemaDTO);
    JsonNodeUtils.merge(stageDefinitionsNode.get(partialSchemaDTO.getNamespace()), pipelineStepsCopy);
  }

  private void mergeStageElementConfig(ObjectNode src, SubtypeClassMap subtypeClassMap) {
    ObjectNode stageElementCopy = src.deepCopy();
    modifyStageElementConfig(stageElementCopy, subtypeClassMap);

    JsonNodeUtils.merge(src, stageElementCopy);
  }

  private ObjectNode obtainPipelineStepsCopyWithCorrectNamespace(
      ObjectNode pipelineSteps, PartialSchemaDTO partialSchemaDTO) {
    ObjectNode pipelineStepsCopy = pipelineSteps.deepCopy();
    yamlSchemaGenerator.modifyRefsNamespace(pipelineStepsCopy, partialSchemaDTO.getNamespace());
    return pipelineStepsCopy;
  }

  private ObjectNode moveRootNodeToDefinitions(String nodeName, ObjectNode nodeSchema, String namespace) {
    ObjectNode definitions = (ObjectNode) nodeSchema.remove(DEFINITIONS_NODE);
    ObjectNode namespaceNode = (ObjectNode) definitions.get(namespace);
    namespaceNode.set(nodeName, nodeSchema);
    return definitions;
  }

  private void modifyStageElementConfig(ObjectNode stageElementConfig, SubtypeClassMap subtypeClassMap) {
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap = new HashMap<>();
    List<Field> fields = YamlSchemaUtils.getTypedFields(STAGE_ELEMENT_CONFIG_CLASS);
    Set<FieldSubtypeData> classFieldSubtypeData =
        fields.stream()
            .map(field -> YamlSchemaUtils.getFieldSubtypeData(field, ImmutableSet.of(subtypeClassMap)))
            .filter(subtype -> subtype.getDiscriminatorType().equals(JsonTypeInfo.As.EXTERNAL_PROPERTY))
            .collect(Collectors.toSet());
    Set<FieldEnumData> fieldEnumData = getFieldEnumData(subtypeClassMap);
    swaggerDefinitionsMetaInfoMap.put(STAGE_ELEMENT_CONFIG,
        SwaggerDefinitionsMetaInfo.builder()
            .fieldEnumData(fieldEnumData)
            .subtypeClassMap(classFieldSubtypeData)
            .build());
    yamlSchemaGenerator.convertSwaggerToJsonSchema(
        swaggerDefinitionsMetaInfoMap, mapper, STAGE_ELEMENT_CONFIG, stageElementConfig);
  }

  private Set<FieldEnumData> getFieldEnumData(SubtypeClassMap subtypeClassMap) {
    return ImmutableSet.of(FieldEnumData.builder()
                               .fieldName("type")
                               .enumValues(ImmutableSet.of(subtypeClassMap.getSubtypeEnum()))
                               .build());
  }

  private Set<FieldEnumData> getFieldEnumData(Field typedField, Set<SubtypeClassMap> mapOfSubtypes) {
    String fieldName = YamlSchemaUtils.getJsonTypeInfo(typedField).property();

    return ImmutableSet.of(
        FieldEnumData.builder()
            .fieldName(fieldName)
            .enumValues(ImmutableSortedSet.copyOf(
                mapOfSubtypes.stream().map(SubtypeClassMap::getSubtypeEnum).collect(Collectors.toList())))
            .build());
  }

  private void flatten(ObjectNode objectNode) {
    JsonNode sections = objectNode.get(PROPERTIES_NODE).get("sections");
    if (sections.isObject()) {
      objectNode.removeAll();
      objectNode.setAll((ObjectNode) sections);
      objectNode.put(SchemaConstants.SCHEMA_NODE, SchemaConstants.JSON_SCHEMA_7);
    }
  }

  private PartialSchemaDTO getStage(String instanceName, String projectIdentifier, String orgIdentifier, Scope scope) {
    try {
      return SafeHttpCall.execute(obtainYamlSchemaClient(instanceName).get(projectIdentifier, orgIdentifier, scope))
          .getData();
    } catch (Exception e) {
      log.warn(
          format("Unable to get %s schema information for projectIdentifier: [%s], orgIdentifier: [%s], scope: [%s]",
              instanceName, projectIdentifier, orgIdentifier, scope),
          e);
      return null;
    }
  }

  public PartialSchemaDTO getApprovalStage(String projectIdentifier, String orgIdentifier, Scope scope) {
    JsonNode approvalStageSchema =
        yamlSchemaProvider.getYamlSchema(EntityType.APPROVAL_STAGE, orgIdentifier, projectIdentifier, scope);

    JsonNode definitions = approvalStageSchema.get(DEFINITIONS_NODE);

    JsonNode jsonNode = definitions.get(StepElementConfig.class.getSimpleName());
    modifyStepElementSchema((ObjectNode) jsonNode);

    jsonNode = definitions.get(ParallelStepElementConfig.class.getSimpleName());
    if (jsonNode.isObject()) {
      flatten((ObjectNode) jsonNode);
    }

    removeUnwantedNodes(definitions);

    yamlSchemaGenerator.modifyRefsNamespace(approvalStageSchema, APPROVAL_NAMESPACE);
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    JsonNode node = mapper.createObjectNode().set(APPROVAL_NAMESPACE, definitions);

    JsonNode partialApprovalSchema = ((ObjectNode) approvalStageSchema).set(DEFINITIONS_NODE, node);

    return PartialSchemaDTO.builder()
        .namespace(APPROVAL_NAMESPACE)
        .nodeName(APPROVAL_STAGE_CONFIG)
        .schema(partialApprovalSchema)
        .nodeType(getApprovalStageTypeName())
        .build();
  }

  private void modifyStepElementSchema(ObjectNode jsonNode) {
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap = new HashMap<>();
    Field typedField = YamlSchemaUtils.getTypedField(STEP_ELEMENT_CONFIG_CLASS);
    Set<SubtypeClassMap> mapOfSubtypes = YamlSchemaUtils.getMapOfSubtypesUsingReflection(typedField);
    Set<FieldSubtypeData> classFieldSubtypeData = new HashSet<>();
    classFieldSubtypeData.add(YamlSchemaUtils.getFieldSubtypeData(typedField, mapOfSubtypes));
    Set<FieldEnumData> fieldEnumData = getFieldEnumData(typedField, mapOfSubtypes);
    swaggerDefinitionsMetaInfoMap.put(STEP_ELEMENT_CONFIG,
        SwaggerDefinitionsMetaInfo.builder()
            .fieldEnumData(fieldEnumData)
            .subtypeClassMap(classFieldSubtypeData)
            .build());
    yamlSchemaGenerator.convertSwaggerToJsonSchema(
        swaggerDefinitionsMetaInfoMap, mapper, STEP_ELEMENT_CONFIG, jsonNode);
  }

  private String getApprovalStageTypeName() {
    JsonTypeName annotation = ApprovalStageConfig.class.getAnnotation(JsonTypeName.class);
    return annotation.value();
  }

  private YamlSchemaClient obtainYamlSchemaClient(String instanceName) {
    return yamlSchemaClientMapper.get(instanceName);
  }

  @Override
  public void validateYamlSchema(String orgId, String projectId, String yaml) {
    try {
      JsonNode schema =
          schemaCache.get(SchemaKey.builder().projectId(projectId).orgId(orgId).scope(Scope.PROJECT).build());
      String schemaString = JsonPipelineUtils.writeJsonString(schema);
      Set<String> errors = yamlSchemaValidator.validate(yaml, schemaString);
      if (!errors.isEmpty()) {
        throw new JsonSchemaValidationException(String.join("\n", errors));
      }
    } catch (Exception ex) {
      log.error(ex.getMessage());
      throw new JsonSchemaValidationException(ex.getMessage());
    }
  }

  private void removeUnwantedNodes(JsonNode definitions) {
    if (definitions.isObject()) {
      Iterator<JsonNode> elements = definitions.elements();
      while (elements.hasNext()) {
        JsonNode jsonNode = elements.next();
        yamlSchemaGenerator.removeUnwantedNodes(jsonNode, YAMLFieldNameConstants.ROLLBACK_STEPS);
      }
    }
  }

  @Value
  @Builder
  @EqualsAndHashCode(callSuper = false)
  private static class SchemaKey {
    String projectId;
    String orgId;
    Scope scope;
  }
}
