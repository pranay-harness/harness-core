package io.harness.pms.pipeline.service;

import static io.harness.yaml.schema.beans.SchemaConstants.DEFINITIONS_NODE;

import static java.lang.String.format;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidYamlException;
import io.harness.exception.JsonSchemaException;
import io.harness.exception.JsonSchemaValidationException;
import io.harness.jackson.JsonNodeUtils;
import io.harness.manage.ManagedExecutorService;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.helpers.PmsFeatureFlagHelper;
import io.harness.pms.merger.helpers.FQNUtils;
import io.harness.pms.pipeline.service.yamlschema.PmsYamlSchemaHelper;
import io.harness.pms.pipeline.service.yamlschema.SchemaFetcher;
import io.harness.pms.pipeline.service.yamlschema.approval.ApprovalYamlSchemaService;
import io.harness.pms.pipeline.service.yamlschema.featureflag.FeatureFlagYamlService;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.YamlUtils;
import io.harness.yaml.schema.YamlSchemaGenerator;
import io.harness.yaml.schema.YamlSchemaProvider;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.utils.JsonPipelineUtils;
import io.harness.yaml.utils.YamlSchemaUtils;
import io.harness.yaml.validator.YamlSchemaValidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class PMSYamlSchemaServiceImpl implements PMSYamlSchemaService {
  private final Executor executor = new ManagedExecutorService(Executors.newFixedThreadPool(4));

  public static final String STAGE_ELEMENT_CONFIG = YamlSchemaUtils.getSwaggerName(StageElementConfig.class);
  public static final Class<StageElementConfig> STAGE_ELEMENT_CONFIG_CLASS = StageElementConfig.class;

  private final YamlSchemaProvider yamlSchemaProvider;
  private final YamlSchemaGenerator yamlSchemaGenerator;
  private final YamlSchemaValidator yamlSchemaValidator;
  private final PmsSdkInstanceService pmsSdkInstanceService;
  private final PmsFeatureFlagHelper pmsFeatureFlagHelper;
  private final PmsYamlSchemaHelper pmsYamlSchemaHelper;
  private final ApprovalYamlSchemaService approvalYamlSchemaService;
  private final FeatureFlagYamlService featureFlagYamlService;
  private final SchemaFetcher schemaFetcher;

  @Inject
  public PMSYamlSchemaServiceImpl(YamlSchemaProvider yamlSchemaProvider, YamlSchemaGenerator yamlSchemaGenerator,
      YamlSchemaValidator yamlSchemaValidator, PmsSdkInstanceService pmsSdkInstanceService,
      PmsFeatureFlagHelper pmsFeatureFlagHelper, PmsYamlSchemaHelper pmsYamlSchemaHelper,
      ApprovalYamlSchemaService approvalYamlSchemaService, FeatureFlagYamlService featureFlagYamlService,
      SchemaFetcher schemaFetcher) {
    this.yamlSchemaProvider = yamlSchemaProvider;
    this.yamlSchemaGenerator = yamlSchemaGenerator;
    this.yamlSchemaValidator = yamlSchemaValidator;
    this.pmsSdkInstanceService = pmsSdkInstanceService;
    this.pmsFeatureFlagHelper = pmsFeatureFlagHelper;
    this.pmsYamlSchemaHelper = pmsYamlSchemaHelper;
    this.approvalYamlSchemaService = approvalYamlSchemaService;
    this.featureFlagYamlService = featureFlagYamlService;
    this.schemaFetcher = schemaFetcher;
  }

  @Override
  public JsonNode getPipelineYamlSchema(String projectIdentifier, String orgIdentifier, Scope scope) {
    try {
      return getPipelineYamlSchemaInternal(projectIdentifier, orgIdentifier, scope);
    } catch (Exception e) {
      log.error("[PMS] Failed to get pipeline yaml schema");
      throw new JsonSchemaException(e.getMessage());
    }
  }

  @Override
  public void validateYamlSchema(String orgId, String projectId, String yaml) {
    try {
      JsonNode schema = getPipelineYamlSchema(projectId, orgId, Scope.PROJECT);
      String schemaString = JsonPipelineUtils.writeJsonString(schema);
      Set<String> errors = yamlSchemaValidator.validate(yaml, schemaString);
      if (!errors.isEmpty()) {
        throw new JsonSchemaValidationException(String.join("\n", errors));
      }
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
      throw new JsonSchemaValidationException(ex.getMessage(), ex);
    }
  }

  @Override
  public void validateYamlSchema(String accountId, String orgId, String projectId, String yaml) {
    if (pmsFeatureFlagHelper.isEnabled(accountId, FeatureName.NG_SCHEMA_VALIDATION)) {
      validateYamlSchema(orgId, projectId, yaml);
    }
  }

  @Override
  public void validateUniqueFqn(String yaml) {
    try {
      FQNUtils.generateFQNMap(YamlUtils.readTree(yaml).getNode().getCurrJsonNode());
    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
    }
  }

  @Override
  public void invalidateAllCache() {
    schemaFetcher.invalidateAllCache();
  }

  private JsonNode getPipelineYamlSchemaInternal(String projectIdentifier, String orgIdentifier, Scope scope) {
    JsonNode pipelineSchema =
        yamlSchemaProvider.getYamlSchema(EntityType.PIPELINES, orgIdentifier, projectIdentifier, scope);

    JsonNode pipelineSteps =
        yamlSchemaProvider.getYamlSchema(EntityType.PIPELINE_STEPS, orgIdentifier, projectIdentifier, scope);
    ObjectNode pipelineDefinitions = (ObjectNode) pipelineSchema.get(DEFINITIONS_NODE);
    ObjectNode pipelineStepsDefinitions = (ObjectNode) pipelineSteps.get(DEFINITIONS_NODE);

    ObjectNode mergedDefinitions = (ObjectNode) JsonNodeUtils.merge(pipelineDefinitions, pipelineStepsDefinitions);

    ObjectNode stageElementConfig = (ObjectNode) pipelineDefinitions.get(STAGE_ELEMENT_CONFIG);

    PmsYamlSchemaHelper.flattenParallelElementConfig(pipelineDefinitions);

    Set<String> instanceNames = pmsSdkInstanceService.getInstanceNames();
    instanceNames.remove(ModuleType.PMS.name().toLowerCase());

    // here for backward compatibility
    instanceNames.remove("pmsInternal");

    CompletableFutures<PartialSchemaDTO> completableFutures = new CompletableFutures<>(executor);
    for (String instanceName : instanceNames) {
      completableFutures.supplyAsync(() -> schemaFetcher.fetchSchema(ModuleType.fromString(instanceName)));
    }

    try {
      List<PartialSchemaDTO> partialSchemaDTOS = completableFutures.allOf().get(2, TimeUnit.MINUTES);
      Map<ModuleType, PartialSchemaDTO> partialSchemaDtoMap =
          partialSchemaDTOS.stream()
              .filter(Objects::nonNull)
              .collect(Collectors.toMap(PartialSchemaDTO::getModuleType, Function.identity()));
      mergeCVIntoCDIfPresent(partialSchemaDtoMap);

      partialSchemaDtoMap.values().forEach(partialSchemaDTO
          -> pmsYamlSchemaHelper.processPartialStageSchema(
              mergedDefinitions, pipelineStepsDefinitions, stageElementConfig, partialSchemaDTO));
    } catch (Exception e) {
      log.error(format("[PMS] Exception while merging yaml schema: %s", e.getMessage()), e);
    }

    pmsYamlSchemaHelper.processPartialStageSchema(mergedDefinitions, pipelineStepsDefinitions, stageElementConfig,
        approvalYamlSchemaService.getApprovalYamlSchema(projectIdentifier, orgIdentifier, scope));

    pmsYamlSchemaHelper.processPartialStageSchema(mergedDefinitions, pipelineStepsDefinitions, stageElementConfig,
        featureFlagYamlService.getFeatureFlagYamlSchema(projectIdentifier, orgIdentifier, scope));

    return ((ObjectNode) pipelineSchema).set(DEFINITIONS_NODE, pipelineDefinitions);
  }

  private void mergeCVIntoCDIfPresent(Map<ModuleType, PartialSchemaDTO> partialSchemaDTOMap) {
    if (!partialSchemaDTOMap.containsKey(ModuleType.CD) || !partialSchemaDTOMap.containsKey(ModuleType.CV)) {
      partialSchemaDTOMap.remove(ModuleType.CV);
      return;
    }

    PartialSchemaDTO cdPartialSchemaDTO = partialSchemaDTOMap.get(ModuleType.CD);
    PartialSchemaDTO cvPartialSchemaDTO = partialSchemaDTOMap.get(ModuleType.CV);

    JsonNode cvDefinitions =
        cvPartialSchemaDTO.getSchema().get(DEFINITIONS_NODE).get(cvPartialSchemaDTO.getNamespace());
    JsonNode cdDefinitions =
        cdPartialSchemaDTO.getSchema().get(DEFINITIONS_NODE).get(cdPartialSchemaDTO.getNamespace());

    JsonNodeUtils.merge(cdDefinitions, cvDefinitions);
    yamlSchemaGenerator.modifyRefsNamespace(cdDefinitions, cdPartialSchemaDTO.getNamespace());

    partialSchemaDTOMap.remove(ModuleType.CV);
  }
}
