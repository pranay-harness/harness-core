package io.harness.pms.pipeline.service;

import static io.harness.yaml.schema.beans.SchemaConstants.DEFINITIONS_NODE;

import static java.lang.String.format;

import io.harness.EntityType;
import io.harness.encryption.Scope;
import io.harness.jackson.JsonNodeUtils;
import io.harness.network.SafeHttpCall;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.yaml.schema.SchemaGeneratorUtils;
import io.harness.yaml.schema.YamlSchemaGenerator;
import io.harness.yaml.schema.YamlSchemaProvider;
import io.harness.yaml.schema.beans.FieldSubtypeData;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.SubtypeClassMap;
import io.harness.yaml.schema.beans.SwaggerDefinitionsMetaInfo;
import io.harness.yaml.schema.client.YamlSchemaClient;
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
import javax.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class PMSYamlSchemaServiceImpl implements PMSYamlSchemaService {
  public static final String STAGE_ELEMENT_CONFIG = StageElementConfig.class.getSimpleName();
  public static final Class<StageElementConfig> STAGE_ELEMENT_CONFIG_CLASS = StageElementConfig.class;
  private final YamlSchemaProvider yamlSchemaProvider;
  private final YamlSchemaGenerator yamlSchemaGenerator;
  private final YamlSchemaClient yamlSchemaClient;

  private final PmsSdkInstanceService pmsSdkInstanceService;

  public JsonNode getPipelineYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope) {
    JsonNode pipelineSchema =
        yamlSchemaProvider.getYamlSchema(EntityType.PIPELINES, orgIdentifier, projectIdentifier, scope);

    JsonNode pipelineDefinitions = pipelineSchema.get(DEFINITIONS_NODE);
    ObjectNode stageElementConfig = (ObjectNode) pipelineDefinitions.get(STAGE_ELEMENT_CONFIG);

    Set<SubtypeClassMap> subtypeClassMapSet = new HashSet<>();
    Set<String> instanceNames = pmsSdkInstanceService.getInstanceNames();
    for (String instanceName : instanceNames) {
      if (instanceName.equals("ci")) {
        PartialSchemaDTO partialSchemaDTO = getCIStage(projectIdentifier, orgIdentifier, scope);
        subtypeClassMapSet.add(SubtypeClassMap.builder()
                                   .subTypeDefinitionKey(partialSchemaDTO.getNodeName())
                                   .subtypeEnum(partialSchemaDTO.getNodeType())
                                   .build());
        ObjectNode stageDefinitionsNode =
            moveRootNodeToDefinitions(partialSchemaDTO.getNodeName(), (ObjectNode) partialSchemaDTO.getSchema());
        JsonNodeUtils.merge(pipelineDefinitions, stageDefinitionsNode);
      }
    }
    modifyStageElementConfig(stageElementConfig, subtypeClassMapSet);
    return ((ObjectNode) pipelineSchema).set(DEFINITIONS_NODE, pipelineDefinitions);
  }

  private ObjectNode moveRootNodeToDefinitions(String nodeName, ObjectNode nodeSchema) {
    ObjectNode definitions = (ObjectNode) nodeSchema.remove(DEFINITIONS_NODE);
    definitions.set(nodeName, nodeSchema);
    return definitions;
  }

  private void modifyStageElementConfig(ObjectNode stageElementConfig, Set<SubtypeClassMap> subtypeClassMapSet) {
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap = new HashMap<>();
    Set<FieldSubtypeData> classFieldSubtypeData = new HashSet<>();
    Field field = YamlSchemaUtils.getTypedField(STAGE_ELEMENT_CONFIG_CLASS);
    classFieldSubtypeData.add(YamlSchemaUtils.getFieldSubtypeData(field, subtypeClassMapSet));
    swaggerDefinitionsMetaInfoMap.put(
        STAGE_ELEMENT_CONFIG, SwaggerDefinitionsMetaInfo.builder().subtypeClassMap(classFieldSubtypeData).build());
    yamlSchemaGenerator.convertSwaggerToJsonSchema(
        swaggerDefinitionsMetaInfoMap, mapper, STAGE_ELEMENT_CONFIG, stageElementConfig);
  }

  private PartialSchemaDTO getCIStage(String projectIdentifier, String orgIdentifier, Scope scope) {
    try {
      return SafeHttpCall.execute(yamlSchemaClient.get(projectIdentifier, orgIdentifier, scope)).getData();
    } catch (Exception e) {
      throw new NotFoundException(
          format("Unable to get ci schema information for projectIdentifier: [%s], orgIdentifier: [%s], scope: [%s]",
              projectIdentifier, orgIdentifier, scope),
          e);
    }
  }
}
