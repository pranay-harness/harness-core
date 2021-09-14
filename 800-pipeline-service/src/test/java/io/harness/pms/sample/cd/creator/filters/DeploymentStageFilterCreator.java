/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sample.cd.creator.filters;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.sample.cd.beans.DeploymentStage;
import io.harness.pms.sample.cd.beans.Infrastructure;
import io.harness.pms.sample.cd.beans.Service;
import io.harness.pms.sample.cd.beans.ServiceDefinition;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DeploymentStageFilterCreator implements FilterJsonCreator<io.harness.pms.sample.cd.beans.DeploymentStage> {
  @Override
  public Class<io.harness.pms.sample.cd.beans.DeploymentStage> getFieldClass() {
    return io.harness.pms.sample.cd.beans.DeploymentStage.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("stage", Collections.singleton("Deployment"));
  }

  @Override
  public FilterCreationResponse handleNode(
      FilterCreationContext filterCreationContext, DeploymentStage deploymentStage) {
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
