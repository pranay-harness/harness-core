package io.harness.cdng.creator.filters;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceYaml;
import io.harness.encryption.SecretRefData;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.filters.FilterCreatorHelper;
import io.harness.filters.SecretRefExtractorHelper;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.cdng.sample.cd.creator.filters.CdFilter;
import io.harness.pms.cdng.sample.cd.creator.filters.CdFilter.CdFilterBuilder;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.filter.creation.FilterCreationResponse.FilterCreationResponseBuilder;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorFactory;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractorVisitor;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class DeploymentStageFilterJsonCreator implements FilterJsonCreator<StageElementConfig> {
  @Inject private SimpleVisitorFactory simpleVisitorFactory;

  @Override
  public Class<StageElementConfig> getFieldClass() {
    return StageElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("stage", Collections.singleton("Deployment"));
  }

  @Override
  public FilterCreationResponse handleNode(FilterCreationContext filterCreationContext, StageElementConfig yamlField) {
    FilterCreationResponseBuilder creationResponse = FilterCreationResponse.builder();

    YamlField variablesField =
        filterCreationContext.getCurrentField().getNode().getField(YAMLFieldNameConstants.VARIABLES);
    if (variablesField != null) {
      FilterCreatorHelper.checkIfVariableNamesAreValid(variablesField);
    }

    CdFilterBuilder cdFilter = CdFilter.builder();
    DeploymentStageConfig deploymentStageConfig = (DeploymentStageConfig) yamlField.getStageType();
    Set<EntityDetailProtoDTO> referredEntities = getReferences(filterCreationContext.getSetupMetadata().getAccountId(),
        filterCreationContext.getSetupMetadata().getOrgId(), filterCreationContext.getSetupMetadata().getProjectId(),
        deploymentStageConfig, yamlField.getIdentifier());
    referredEntities.addAll(extractSecretRefs(filterCreationContext));
    creationResponse.referredEntities(new ArrayList<>(referredEntities));

    if (deploymentStageConfig.getExecution() == null) {
      throw new InvalidRequestException("Execution section missing from Deployment Stage");
    }

    ServiceYaml service = deploymentStageConfig.getServiceConfig().getService();
    if (service == null
        && (deploymentStageConfig.getServiceConfig().getServiceRef() == null
            || deploymentStageConfig.getServiceConfig().getServiceRef().fetchFinalValue() == null)
        && deploymentStageConfig.getServiceConfig().getUseFromStage() == null) {
      throw new InvalidRequestException("One of service, serviceRef and useFromStage should be present");
    }
    if (service != null && isNotEmpty(service.getName())) {
      cdFilter.serviceName(service.getName());
    }

    ServiceDefinition serviceDefinition = deploymentStageConfig.getServiceConfig().getServiceDefinition();
    if (serviceDefinition != null && serviceDefinition.getType() != null) {
      cdFilter.deploymentType(serviceDefinition.getType());
    }

    PipelineInfrastructure infrastructure = deploymentStageConfig.getInfrastructure();
    if (infrastructure == null) {
      throw new InvalidRequestException("Infrastructure cannot be null");
    }
    if (infrastructure.getEnvironment() == null
        && (infrastructure.getEnvironmentRef() == null || infrastructure.getEnvironmentRef().fetchFinalValue() == null)
        && infrastructure.getUseFromStage() == null) {
      throw new InvalidRequestException("One of environment, environment and useFromStage should be present");
    }

    if (infrastructure.getEnvironment() != null && isNotEmpty(infrastructure.getEnvironment().getName())) {
      cdFilter.environmentName(infrastructure.getEnvironment().getName());
    }

    if (infrastructure.getInfrastructureDefinition() != null
        && isNotEmpty(infrastructure.getInfrastructureDefinition().getType())) {
      cdFilter.infrastructureType(infrastructure.getInfrastructureDefinition().getType());
    }

    creationResponse.dependencies(CDExecutionUtils.getDependencies(filterCreationContext.getCurrentField()));

    creationResponse.pipelineFilter(cdFilter.build());
    return creationResponse.build();
  }

  private Set<EntityDetailProtoDTO> extractSecretRefs(FilterCreationContext context) {
    String accountId = context.getSetupMetadata().getAccountId();
    String orgId = context.getSetupMetadata().getOrgId();
    String projectId = context.getSetupMetadata().getProjectId();
    Set<EntityDetailProtoDTO> entityDetailProtoDTOS = new HashSet<>();
    YamlField variablesField = context.getCurrentField().getNode().getField(YAMLFieldNameConstants.VARIABLES);
    if (variablesField == null) {
      return new HashSet<>();
    }
    Map<String, ParameterField<SecretRefData>> fqnToSecretRefs =
        SecretRefExtractorHelper.extractSecretRefsFromVariables(variablesField);
    for (Map.Entry<String, ParameterField<SecretRefData>> entry : fqnToSecretRefs.entrySet()) {
      entityDetailProtoDTOS.add(FilterCreatorHelper.convertSecretToEntityDetailProtoDTO(
          accountId, orgId, projectId, entry.getKey(), entry.getValue()));
    }
    return entityDetailProtoDTOS;
  }

  private Set<EntityDetailProtoDTO> getReferences(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, DeploymentStageConfig deploymentStageConfig, String stageIdentifier) {
    List<LevelNode> levelNodes = new LinkedList<>();
    levelNodes.add(LevelNode.builder().qualifierName(YAMLFieldNameConstants.PIPELINE).build());
    levelNodes.add(LevelNode.builder().qualifierName(YAMLFieldNameConstants.STAGES).build());
    levelNodes.add(LevelNode.builder().qualifierName(stageIdentifier).build());
    EntityReferenceExtractorVisitor visitor = simpleVisitorFactory.obtainEntityReferenceExtractorVisitor(
        accountIdentifier, orgIdentifier, projectIdentifier, levelNodes);
    visitor.walkElementTree(deploymentStageConfig);
    return visitor.getEntityReferenceSet();
  }
}
