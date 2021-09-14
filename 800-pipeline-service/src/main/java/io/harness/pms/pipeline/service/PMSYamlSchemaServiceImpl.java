/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.pipeline.service;

import static io.harness.yaml.schema.beans.SchemaConstants.ALL_OF_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.DEFINITIONS_NODE;

import static java.lang.String.format;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;
import io.harness.exception.InvalidYamlException;
import io.harness.exception.JsonSchemaException;
import io.harness.exception.JsonSchemaValidationException;
import io.harness.jackson.JsonNodeUtils;
import io.harness.manage.ManagedExecutorService;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.merger.helpers.FQNMapGenerator;
import io.harness.pms.pipeline.service.yamlschema.PmsYamlSchemaHelper;
import io.harness.pms.pipeline.service.yamlschema.SchemaFetcher;
import io.harness.pms.pipeline.service.yamlschema.approval.ApprovalYamlSchemaService;
import io.harness.pms.pipeline.service.yamlschema.featureflag.FeatureFlagYamlService;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.yaml.YamlUtils;
import io.harness.project.remote.ProjectClient;
import io.harness.remote.client.NGRestUtils;
import io.harness.yaml.schema.YamlSchemaGenerator;
import io.harness.yaml.schema.YamlSchemaProvider;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.utils.JsonPipelineUtils;
import io.harness.yaml.utils.YamlSchemaUtils;
import io.harness.yaml.validator.YamlSchemaValidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

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
  private final PmsYamlSchemaHelper pmsYamlSchemaHelper;
  private final ApprovalYamlSchemaService approvalYamlSchemaService;
  private final FeatureFlagYamlService featureFlagYamlService;
  private final SchemaFetcher schemaFetcher;
  private final ProjectClient projectClient;

  @Inject
  public PMSYamlSchemaServiceImpl(YamlSchemaProvider yamlSchemaProvider, YamlSchemaGenerator yamlSchemaGenerator,
      YamlSchemaValidator yamlSchemaValidator, PmsSdkInstanceService pmsSdkInstanceService,
      PmsYamlSchemaHelper pmsYamlSchemaHelper, ApprovalYamlSchemaService approvalYamlSchemaService,
      FeatureFlagYamlService featureFlagYamlService, SchemaFetcher schemaFetcher, ProjectClient projectClient) {
    this.yamlSchemaProvider = yamlSchemaProvider;
    this.yamlSchemaGenerator = yamlSchemaGenerator;
    this.yamlSchemaValidator = yamlSchemaValidator;
    this.pmsSdkInstanceService = pmsSdkInstanceService;
    this.pmsYamlSchemaHelper = pmsYamlSchemaHelper;
    this.approvalYamlSchemaService = approvalYamlSchemaService;
    this.featureFlagYamlService = featureFlagYamlService;
    this.schemaFetcher = schemaFetcher;
    this.projectClient = projectClient;
  }

  @Override
  public JsonNode getPipelineYamlSchema(
      String accountIdentifier, String projectIdentifier, String orgIdentifier, Scope scope) {
    try {
      return getPipelineYamlSchemaInternal(accountIdentifier, projectIdentifier, orgIdentifier, scope);
    } catch (Exception e) {
      log.error("[PMS] Failed to get pipeline yaml schema");
      throw new JsonSchemaException(e.getMessage());
    }
  }

  @Override
  public void validateYamlSchema(String accountId, String orgId, String projectId, String yaml) {
    validateYamlSchemaInternal(accountId, orgId, projectId, yaml);
  }

  private void validateYamlSchemaInternal(String accountIdentifier, String orgId, String projectId, String yaml) {
    try {
      JsonNode schema = getPipelineYamlSchema(accountIdentifier, projectId, orgId, Scope.PROJECT);
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
  public void validateUniqueFqn(String yaml) {
    try {
      FQNMapGenerator.generateFQNMap(YamlUtils.readTree(yaml).getNode().getCurrJsonNode());
    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
    }
  }

  @Override
  public void invalidateAllCache() {
    schemaFetcher.invalidateAllCache();
  }

  private JsonNode getPipelineYamlSchemaInternal(
      String accountIdentifier, String projectIdentifier, String orgIdentifier, Scope scope) {
    JsonNode pipelineSchema =
        yamlSchemaProvider.getYamlSchema(EntityType.PIPELINES, orgIdentifier, projectIdentifier, scope);

    JsonNode pipelineSteps =
        yamlSchemaProvider.getYamlSchema(EntityType.PIPELINE_STEPS, orgIdentifier, projectIdentifier, scope);
    ObjectNode pipelineDefinitions = (ObjectNode) pipelineSchema.get(DEFINITIONS_NODE);
    ObjectNode pipelineStepsDefinitions = (ObjectNode) pipelineSteps.get(DEFINITIONS_NODE);

    ObjectNode mergedDefinitions = (ObjectNode) JsonNodeUtils.merge(pipelineDefinitions, pipelineStepsDefinitions);

    ObjectNode stageElementConfig = (ObjectNode) pipelineDefinitions.get(STAGE_ELEMENT_CONFIG);

    PmsYamlSchemaHelper.flattenParallelElementConfig(pipelineDefinitions);

    List<ModuleType> enabledModules = obtainEnabledModules(projectIdentifier, accountIdentifier, orgIdentifier);

    CompletableFutures<PartialSchemaDTO> completableFutures = new CompletableFutures<>(executor);
    for (ModuleType enabledModule : enabledModules) {
      completableFutures.supplyAsync(() -> schemaFetcher.fetchSchema(enabledModule));
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
    yamlSchemaGenerator.modifyRefsNamespace(cvDefinitions, cdPartialSchemaDTO.getNamespace());

    JsonNode cdDefinitions =
        cdPartialSchemaDTO.getSchema().get(DEFINITIONS_NODE).get(cdPartialSchemaDTO.getNamespace());

    JsonNode cdDefinitionsCopy = cdDefinitions.deepCopy();

    JsonNodeUtils.merge(cdDefinitions, cvDefinitions);

    // TODO(Alexei) This is SOOOO ugly, find better way to do it
    populateAllOfForCD(cdDefinitions, cdDefinitionsCopy);

    partialSchemaDTOMap.remove(ModuleType.CV);
  }

  private void populateAllOfForCD(JsonNode cdDefinitions, JsonNode cdDefinitionsCopy) {
    ArrayNode cdDefinitionsAllOfNode =
        (ArrayNode) cdDefinitions.get(PmsYamlSchemaHelper.STEP_ELEMENT_CONFIG).get(ALL_OF_NODE);
    ArrayNode cdDefinitionsCopyAllOfNode =
        (ArrayNode) cdDefinitionsCopy.get(PmsYamlSchemaHelper.STEP_ELEMENT_CONFIG).get(ALL_OF_NODE);

    for (int i = 0; i < cdDefinitionsCopyAllOfNode.size(); i++) {
      cdDefinitionsAllOfNode.add(cdDefinitionsCopyAllOfNode.get(i));
    }
    JsonNodeUtils.removeDuplicatesFromArrayNode(cdDefinitionsAllOfNode);
  }

  @SuppressWarnings("unchecked")
  private List<ModuleType> obtainEnabledModules(
      String projectIdentifier, String accountIdentifier, String orgIdentifier) {
    try {
      Optional<ProjectResponse> resp =
          NGRestUtils.getResponse(projectClient.getProject(projectIdentifier, accountIdentifier, orgIdentifier));
      if (!resp.isPresent()) {
        log.warn(
            "[PMS] Cannot obtain project details for projectIdentifier : {}, accountIdentifier: {}, orgIdentifier: {}",
            projectIdentifier, accountIdentifier, orgIdentifier);
        return new ArrayList<>();
      }

      List<ModuleType> projectModuleTypes = resp.get()
                                                .getProject()
                                                .getModules()
                                                .stream()
                                                .filter(moduleType -> !moduleType.isInternal())
                                                .collect(Collectors.toList());

      List<ModuleType> instanceModuleTypes = pmsSdkInstanceService.getActiveInstanceNames()
                                                 .stream()
                                                 .map(ModuleType::fromString)
                                                 .collect(Collectors.toList());

      return (List<ModuleType>) CollectionUtils.intersection(projectModuleTypes, instanceModuleTypes);
    } catch (Exception e) {
      log.warn(
          "[PMS] Cannot obtain enabled module details for projectIdentifier : {}, accountIdentifier: {}, orgIdentifier: {}",
          projectIdentifier, accountIdentifier, orgIdentifier, e);
      return new ArrayList<>();
    }
  }
}
