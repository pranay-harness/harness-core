package io.harness.pms.pipeline.service;

import static io.harness.yaml.schema.beans.SchemaConstants.ANY_OF_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.DEFINITIONS_NAMESPACE_STRING_PATTERN;
import static io.harness.yaml.schema.beans.SchemaConstants.DEFINITIONS_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.PROPERTIES_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.REF_NODE;

import static java.lang.String.format;

import io.harness.EntityType;
import io.harness.encryption.Scope;
import io.harness.jackson.JsonNodeUtils;
import io.harness.network.SafeHttpCall;
import io.harness.plancreator.stages.parallel.ParallelStageElementConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.utils.PmsConstants;
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
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class PMSYamlSchemaServiceImpl implements PMSYamlSchemaService {
  public static final String STAGE_ELEMENT_CONFIG = YamlSchemaUtils.getSwaggerName(StageElementConfig.class);
  public static final Class<StageElementConfig> STAGE_ELEMENT_CONFIG_CLASS = StageElementConfig.class;

  private static final String APPROVAL_STAGE_CONFIG = YamlSchemaUtils.getSwaggerName(ApprovalStageConfig.class);
  private static final String STEP_ELEMENT_CONFIG = YamlSchemaUtils.getSwaggerName(StepElementConfig.class);
  private static final Class<StepElementConfig> STEP_ELEMENT_CONFIG_CLASS = StepElementConfig.class;
  private static final String APPROVAL_NAMESPACE = "approval";
  private static final String APPROVAL_INSTANCE_NAME = "approval";

  private final YamlSchemaProvider yamlSchemaProvider;
  private final YamlSchemaGenerator yamlSchemaGenerator;
  private final Map<String, YamlSchemaClient> yamlSchemaClientMapper;

  private final PmsSdkInstanceService pmsSdkInstanceService;

  public JsonNode getPipelineYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope) {
    JsonNode pipelineSchema =
        yamlSchemaProvider.getYamlSchema(EntityType.PIPELINES, orgIdentifier, projectIdentifier, scope);

    ObjectNode pipelineDefinitions = (ObjectNode) pipelineSchema.get(DEFINITIONS_NODE);
    ObjectNode stageElementConfig = (ObjectNode) pipelineDefinitions.remove(STAGE_ELEMENT_CONFIG);

    JsonNode jsonNode = pipelineDefinitions.get(ParallelStageElementConfig.class.getSimpleName());
    if (jsonNode.isObject()) {
      flattenParallelStepElementConfig((ObjectNode) jsonNode);
    }

    Set<String> instanceNames = pmsSdkInstanceService.getInstanceNames();
    Set<String> refs = new HashSet<>();
    for (String instanceName : instanceNames) {
      if (instanceName.equals(PmsConstants.INTERNAL_SERVICE_NAME)) {
        continue;
      }
      PartialSchemaDTO partialSchemaDTO = getStage(instanceName, projectIdentifier, orgIdentifier, scope);
      processPartialStageSchema(pipelineDefinitions, stageElementConfig, refs, instanceName, partialSchemaDTO);
    }
    processPartialStageSchema(pipelineDefinitions, stageElementConfig, refs, APPROVAL_INSTANCE_NAME,
        getApprovalStage(projectIdentifier, orgIdentifier, scope));

    ObjectNode stageElementWrapperConfig = (ObjectNode) pipelineDefinitions.get("StageElementWrapperConfig");
    modifyStageElementWrapperConfig(stageElementWrapperConfig, refs);
    return ((ObjectNode) pipelineSchema).set(DEFINITIONS_NODE, pipelineDefinitions);
  }

  private void processPartialStageSchema(ObjectNode pipelineDefinitions, ObjectNode stageElementConfig,
      Set<String> refs, String instanceName, PartialSchemaDTO partialSchemaDTO) {
    SubtypeClassMap subtypeClassMap =
        SubtypeClassMap.builder()
            .subTypeDefinitionKey(partialSchemaDTO.getNamespace() + "/" + partialSchemaDTO.getNodeName())
            .subtypeEnum(partialSchemaDTO.getNodeType())
            .build();
    ObjectNode stageDefinitionsNode = moveRootNodeToDefinitions(
        partialSchemaDTO.getNodeName(), (ObjectNode) partialSchemaDTO.getSchema(), partialSchemaDTO.getNamespace());
    ObjectNode stageElementCopy = stageElementConfig.deepCopy();
    modifyStageElementConfig(stageElementCopy, subtypeClassMap,
        instanceName.equals("ci")                         ? Arrays.asList("rollbackSteps", "failureStrategies")
            : instanceName.equals(APPROVAL_INSTANCE_NAME) ? Collections.singletonList("rollbackSteps")
                                                          : Collections.emptyList());

    ObjectNode namespaceNode = (ObjectNode) stageDefinitionsNode.get(partialSchemaDTO.getNamespace());
    namespaceNode.set(STAGE_ELEMENT_CONFIG, stageElementCopy);
    refs.add(format(DEFINITIONS_NAMESPACE_STRING_PATTERN, partialSchemaDTO.getNamespace(), STAGE_ELEMENT_CONFIG));
    JsonNodeUtils.merge(pipelineDefinitions, stageDefinitionsNode);
  }

  private void modifyStageElementWrapperConfig(ObjectNode node, Set<String> refs) {
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    ArrayNode refsArray = mapper.createArrayNode();
    refs.forEach(s -> refsArray.add(mapper.createObjectNode().put(REF_NODE, s)));
    ObjectNode stage = mapper.createObjectNode();
    stage.set(ANY_OF_NODE, refsArray);
    node.remove("stage");
    ObjectNode properties = (ObjectNode) node.get(PROPERTIES_NODE);
    properties.set("stage", stage);
  }

  private ObjectNode moveRootNodeToDefinitions(String nodeName, ObjectNode nodeSchema, String namespace) {
    ObjectNode definitions = (ObjectNode) nodeSchema.remove(DEFINITIONS_NODE);
    ObjectNode namespaceNode = (ObjectNode) definitions.get(namespace);
    namespaceNode.set(nodeName, nodeSchema);
    return definitions;
  }

  private void modifyStageElementConfig(
      ObjectNode stageElementConfig, SubtypeClassMap subtypeClassMap, List<String> unwantedNodes) {
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap = new HashMap<>();
    Set<FieldSubtypeData> classFieldSubtypeData = new HashSet<>();
    Field field = YamlSchemaUtils.getTypedField(STAGE_ELEMENT_CONFIG_CLASS);
    classFieldSubtypeData.add(YamlSchemaUtils.getFieldSubtypeData(field, ImmutableSet.of(subtypeClassMap)));
    Set<FieldEnumData> fieldEnumData = getFieldEnumData(subtypeClassMap);
    swaggerDefinitionsMetaInfoMap.put(STAGE_ELEMENT_CONFIG,
        SwaggerDefinitionsMetaInfo.builder()
            .fieldEnumData(fieldEnumData)
            .subtypeClassMap(classFieldSubtypeData)
            .build());
    yamlSchemaGenerator.convertSwaggerToJsonSchema(
        swaggerDefinitionsMetaInfoMap, mapper, STAGE_ELEMENT_CONFIG, stageElementConfig);
    JsonNode propertiesNode = stageElementConfig.get(PROPERTIES_NODE);
    if (propertiesNode.isObject()) {
      unwantedNodes.forEach(((ObjectNode) propertiesNode)::remove);
    }
  }

  private Set<FieldEnumData> getFieldEnumData(SubtypeClassMap subtypeClassMap) {
    return ImmutableSet.of(FieldEnumData.builder()
                               .fieldName("type")
                               .enumValues(ImmutableSet.of(subtypeClassMap.getSubtypeEnum()))
                               .build());
  }

  private void flattenParallelStepElementConfig(ObjectNode objectNode) {
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
      throw new NotFoundException(
          format("Unable to get %s schema information for projectIdentifier: [%s], orgIdentifier: [%s], scope: [%s]",
              instanceName, projectIdentifier, orgIdentifier, scope),
          e);
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
      flattenParallelStepElementConfig((ObjectNode) jsonNode);
    }

    yamlSchemaGenerator.modifyRefsNamespace(approvalStageSchema, APPROVAL_NAMESPACE);
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    JsonNode node = mapper.createObjectNode().set(APPROVAL_NAMESPACE, definitions);

    JsonNode partialCdSchema = ((ObjectNode) approvalStageSchema).set(DEFINITIONS_NODE, node);

    return PartialSchemaDTO.builder()
        .namespace(APPROVAL_NAMESPACE)
        .nodeName(APPROVAL_STAGE_CONFIG)
        .schema(partialCdSchema)
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
    swaggerDefinitionsMetaInfoMap.put(
        STEP_ELEMENT_CONFIG, SwaggerDefinitionsMetaInfo.builder().subtypeClassMap(classFieldSubtypeData).build());
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
}
