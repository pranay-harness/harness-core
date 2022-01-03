package io.harness.pms.pipeline.service.yamlschema.featureflag;

import static io.harness.yaml.schema.beans.SchemaConstants.DEFINITIONS_NODE;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cf.pipeline.FeatureFlagStageConfig;
import io.harness.encryption.Scope;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.pipeline.service.yamlschema.PmsYamlSchemaHelper;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.yaml.schema.SchemaGeneratorUtils;
import io.harness.yaml.schema.YamlSchemaGenerator;
import io.harness.yaml.schema.YamlSchemaProvider;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class FeatureFlagYamlServiceImpl implements FeatureFlagYamlService {
  private static final String FEATURE_FLAG_STAGE_CONFIG = YamlSchemaUtils.getSwaggerName(FeatureFlagStageConfig.class);
  private static final String FEATURE_FLAG_NAMESPACE = "cf";

  @Inject private YamlSchemaProvider yamlSchemaProvider;
  @Inject private PmsYamlSchemaHelper pmsYamlSchemaHelper;
  @Inject private YamlSchemaGenerator yamlSchemaGenerator;
  @Inject private List<YamlSchemaRootClass> yamlSchemaRootClasses;

  @Override
  public PartialSchemaDTO getFeatureFlagYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope) {
    JsonNode featureFlagStageSchema =
        yamlSchemaProvider.getYamlSchema(EntityType.FEATURE_FLAG_STAGE, orgIdentifier, projectIdentifier, scope);

    JsonNode definitions = featureFlagStageSchema.get(DEFINITIONS_NODE);

    JsonNode jsonNode = definitions.get(StepElementConfig.class.getSimpleName());
    pmsYamlSchemaHelper.modifyStepElementSchema((ObjectNode) jsonNode);

    jsonNode = definitions.get(ParallelStepElementConfig.class.getSimpleName());
    if (jsonNode.isObject()) {
      PmsYamlSchemaHelper.flatten((ObjectNode) jsonNode);
    }

    pmsYamlSchemaHelper.removeUnwantedNodes(definitions, ImmutableSet.of(YAMLFieldNameConstants.ROLLBACK_STEPS));
    yamlSchemaProvider.mergeAllV2StepsDefinitions(projectIdentifier, orgIdentifier, scope, (ObjectNode) definitions,
        YamlSchemaUtils.getNodeEntityTypesByYamlGroup(yamlSchemaRootClasses, StepCategory.STEP.name()));

    YamlSchemaUtils.addOneOfInExecutionWrapperConfig(featureFlagStageSchema.get(DEFINITIONS_NODE),
        YamlSchemaUtils.getNodeClassesByYamlGroup(yamlSchemaRootClasses, StepCategory.STEP.name()), "");

    yamlSchemaGenerator.modifyRefsNamespace(featureFlagStageSchema, FEATURE_FLAG_NAMESPACE);
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    JsonNode node = mapper.createObjectNode().set(FEATURE_FLAG_NAMESPACE, definitions);

    JsonNode partialApprovalSchema = ((ObjectNode) featureFlagStageSchema).set(DEFINITIONS_NODE, node);

    return PartialSchemaDTO.builder()
        .namespace(FEATURE_FLAG_NAMESPACE)
        .nodeName(FEATURE_FLAG_STAGE_CONFIG)
        .schema(partialApprovalSchema)
        .nodeType(getFeatureFlagStageTypeName())
        .moduleType(ModuleType.PMS)
        .build();
  }

  private String getFeatureFlagStageTypeName() {
    JsonTypeName annotation = FeatureFlagStageConfig.class.getAnnotation(JsonTypeName.class);
    return annotation.value();
  }
}
