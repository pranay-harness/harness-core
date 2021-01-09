package io.harness.cdng.creator.filters;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.cdng.sample.cd.creator.filters.CdFilter;
import io.harness.pms.cdng.sample.cd.creator.filters.CdFilter.CdFilterBuilder;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.filter.creation.FilterCreationResponse.FilterCreationResponseBuilder;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DeploymentStageFilterJsonCreator implements FilterJsonCreator<StageElementConfig> {
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

    CdFilterBuilder cdFilter = CdFilter.builder();
    DeploymentStageConfig deploymentStageConfig = (DeploymentStageConfig) yamlField.getStageType();
    if (deploymentStageConfig.getExecution() == null) {
      return creationResponse.build();
    }

    ServiceConfig service = deploymentStageConfig.getService();
    if (service != null && isNotEmpty(service.getName().getValue())) {
      cdFilter.serviceName(service.getName().getValue());
    }

    ServiceDefinition serviceDefinition = service.getServiceDefinition();
    if (serviceDefinition != null && serviceDefinition.getType() != null) {
      cdFilter.deploymentType(serviceDefinition.getType());
    }

    PipelineInfrastructure infrastructure = deploymentStageConfig.getInfrastructure();
    if (infrastructure != null && infrastructure.getEnvironment() != null
        && isNotEmpty(infrastructure.getEnvironment().getName().getValue())) {
      cdFilter.environmentName(infrastructure.getEnvironment().getName().getValue());
    }

    creationResponse.pipelineFilter(cdFilter.build());
    return creationResponse.build();
  }
}
