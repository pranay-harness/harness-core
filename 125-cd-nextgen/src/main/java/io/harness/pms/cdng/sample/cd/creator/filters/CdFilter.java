/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.cdng.sample.cd.creator.filters;

import io.harness.pms.pipeline.filter.PipelineFilter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
@JsonInclude(Include.NON_NULL)
public class CdFilter implements PipelineFilter {
  @Singular Set<String> deploymentTypes;
  @Singular Set<String> environmentNames;
  @Singular Set<String> serviceNames;
  @Singular Set<String> infrastructureTypes;

  public void addDeploymentTypes(Set<String> deploymentTypes) {
    if (this.deploymentTypes == null) {
      this.deploymentTypes = new HashSet<>();
    } else if (!(this.deploymentTypes instanceof HashSet)) {
      this.deploymentTypes = new HashSet<>(this.deploymentTypes);
    }

    this.deploymentTypes.addAll(deploymentTypes);
  }

  public void addInfrastructureTypes(Set<String> infrastructureTypes) {
    if (this.infrastructureTypes == null) {
      this.infrastructureTypes = new HashSet<>();
    } else if (!(this.infrastructureTypes instanceof HashSet)) {
      this.infrastructureTypes = new HashSet<>(this.infrastructureTypes);
    }
    this.infrastructureTypes.addAll(infrastructureTypes);
  }
  public void addServiceNames(Set<String> serviceNames) {
    if (this.serviceNames == null) {
      this.serviceNames = new HashSet<>();
    } else if (!(this.serviceNames instanceof HashSet)) {
      this.serviceNames = new HashSet<>(this.serviceNames);
    }

    this.serviceNames.addAll(serviceNames);
  }

  public void addEnvironmentNames(Set<String> environmentNames) {
    if (this.environmentNames == null) {
      this.environmentNames = new HashSet<>();
    } else if (!(this.environmentNames instanceof HashSet)) {
      this.environmentNames = new HashSet<>(this.environmentNames);
    }

    this.environmentNames.addAll(environmentNames);
  }
}
