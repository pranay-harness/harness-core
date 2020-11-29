package io.harness.pms.sample.cd.creator.filters;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.pms.filter.FilterCreationResponse;
import io.harness.pms.plan.creator.filters.FilterJsonCreator;
import io.harness.pms.sample.cd.beans.DeploymentStage;
import io.harness.pms.sample.cd.beans.Infrastructure;
import io.harness.pms.sample.cd.beans.Service;
import io.harness.pms.sample.cd.beans.ServiceDefinition;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DeploymentStageFilterCreator implements FilterJsonCreator<DeploymentStage> {
  @Override
  public Class<DeploymentStage> getFieldClass() {
    return DeploymentStage.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("stage", Collections.singleton("Deployment"));
  }

  @Override
  public FilterCreationResponse handleNode(DeploymentStage deploymentStage) {
    FilterCreationResponse creationResponse = FilterCreationResponse.builder().build();

    CdFilter cdFilter = CdFilter.builder().build();
    if (deploymentStage.getSpec() == null) {
      return creationResponse;
    }

    Service service = deploymentStage.getSpec().getService();
    if (service != null && isNotEmpty(service.getName())) {
      cdFilter.addServiceName(service.getName());
    }

    ServiceDefinition serviceDefinition = service.getServiceDefinition();
    if (serviceDefinition != null && serviceDefinition.getType() != null) {
      cdFilter.addDeploymentType(serviceDefinition.getType());
    }

    Infrastructure infrastructure = deploymentStage.getSpec().getInfrastructure();
    if (infrastructure != null && infrastructure.getEnvironment() != null
        && isNotEmpty(infrastructure.getEnvironment().getName())) {
      cdFilter.addEnvironmentName(infrastructure.getEnvironment().getName());
    }

    creationResponse.setPipelineFilter(cdFilter);
    creationResponse.setStageCount(1);
    return creationResponse;
  }
}
