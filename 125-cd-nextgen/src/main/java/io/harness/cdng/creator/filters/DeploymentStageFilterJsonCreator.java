package io.harness.cdng.creator.filters;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceYaml;
import io.harness.eraro.ErrorCode;
import io.harness.exception.FilterCreatorException;
import io.harness.filters.GenericStageFilterJsonCreator;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.cdng.sample.cd.creator.filters.CdFilter;
import io.harness.pms.cdng.sample.cd.creator.filters.CdFilter.CdFilterBuilder;
import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.CDC)
public class DeploymentStageFilterJsonCreator extends GenericStageFilterJsonCreator {
  @Override
  public Set<String> getSupportedStageTypes() {
    return Collections.singleton("Deployment");
  }

  @Override
  public PipelineFilter getFilter(FilterCreationContext filterCreationContext, StageElementConfig yamlField) {
    CdFilterBuilder cdFilter = CdFilter.builder();
    DeploymentStageConfig deploymentStageConfig = (DeploymentStageConfig) yamlField.getStageType();

    ServiceYaml service = deploymentStageConfig.getServiceConfig().getService();
    if (service == null
        && (deploymentStageConfig.getServiceConfig().getServiceRef() == null
            || deploymentStageConfig.getServiceConfig().getServiceRef().fetchFinalValue() == null)
        && deploymentStageConfig.getServiceConfig().getUseFromStage() == null) {
      throw new FilterCreatorException(
          format(
              "One of service, serviceRef and useFromStage should be present in stage [%s]. Please add it and try again",
              YamlUtils.getFullyQualifiedName(filterCreationContext.getCurrentField().getNode())),
          ErrorCode.INVALID_YAML_ERROR);
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
      throw new FilterCreatorException(
          format("Infrastructure cannot be null in stage [%s]. Please add it and try again",
              YamlUtils.getFullyQualifiedName(filterCreationContext.getCurrentField().getNode())),
          ErrorCode.INVALID_YAML_ERROR);
    }
    if (infrastructure.getEnvironment() == null
        && (infrastructure.getEnvironmentRef() == null || infrastructure.getEnvironmentRef().fetchFinalValue() == null)
        && infrastructure.getUseFromStage() == null) {
      throw new FilterCreatorException(
          format(
              "One of environment, environment and useFromStage should be present in stage [%s]. Please add it and try again",
              YamlUtils.getFullyQualifiedName(filterCreationContext.getCurrentField().getNode())),
          ErrorCode.INVALID_YAML_ERROR);
    }

    if (infrastructure.getEnvironment() != null && isNotEmpty(infrastructure.getEnvironment().getName())) {
      cdFilter.environmentName(infrastructure.getEnvironment().getName());
    }

    if (infrastructure.getInfrastructureDefinition() != null
        && isNotEmpty(infrastructure.getInfrastructureDefinition().getType())) {
      cdFilter.infrastructureType(infrastructure.getInfrastructureDefinition().getType());
    }
    return cdFilter.build();
  }

  @Override
  @NotNull
  protected Map<String, YamlField> getDependencies(YamlField stageField) {
    // Add dependency for rollback steps
    Map<String, YamlField> dependencies = new HashMap<>(super.getDependencies(stageField));
    YamlField executionField =
        stageField.getNode().getField(YAMLFieldNameConstants.SPEC).getNode().getField(YAMLFieldNameConstants.EXECUTION);
    YamlField rollbackStepsField = executionField.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);
    if (rollbackStepsField != null && rollbackStepsField.getNode().asArray().size() != 0) {
      addRollbackDependencies(dependencies, rollbackStepsField);
    }
    return dependencies;
  }

  private void addRollbackDependencies(Map<String, YamlField> dependencies, YamlField rollbackStepsField) {
    List<YamlField> stepYamlFields = PlanCreatorUtils.getStepYamlFields(rollbackStepsField.getNode().asArray());
    for (YamlField stepYamlField : stepYamlFields) {
      dependencies.put(stepYamlField.getNode().getUuid(), stepYamlField);
    }
  }
}
