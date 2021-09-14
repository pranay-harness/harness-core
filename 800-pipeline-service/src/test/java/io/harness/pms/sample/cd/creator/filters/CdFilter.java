/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sample.cd.creator.filters;

import io.harness.pms.pipeline.filter.PipelineFilter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(Include.NON_NULL)
public class CdFilter implements PipelineFilter {
  Set<String> deploymentTypes;
  Set<String> environmentNames;
  Set<String> serviceNames;

  public void addDeploymentType(String deploymentType) {
    if (deploymentTypes == null) {
      deploymentTypes = new HashSet<>();
    }

    deploymentTypes.add(deploymentType);
  }

  public void addDeploymentTypes(Set<String> deploymentTypes) {
    if (this.deploymentTypes == null) {
      this.deploymentTypes = new HashSet<>();
    }

    this.deploymentTypes.addAll(deploymentTypes);
  }

  public void addServiceName(String serviceName) {
    if (serviceNames == null) {
      serviceNames = new HashSet<>();
    }

    serviceNames.add(serviceName);
  }

  public void addServiceNames(Set<String> serviceNames) {
    if (this.serviceNames == null) {
      this.serviceNames = new HashSet<>();
    }

    this.serviceNames.addAll(serviceNames);
  }

  public void addEnvironmentName(String environmentName) {
    if (environmentNames == null) {
      environmentNames = new HashSet<>();
    }

    environmentNames.add(environmentName);
  }

  public void addEnvironmentNames(Set<String> environmentNames) {
    if (this.environmentNames == null) {
      this.environmentNames = new HashSet<>();
    }

    this.environmentNames.addAll(environmentNames);
  }
}
