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

  public void addDeploymentTypes(Set<String> deploymentTypes) {
    if (this.deploymentTypes == null) {
      this.deploymentTypes = new HashSet<>();
    }

    this.deploymentTypes.addAll(deploymentTypes);
  }

  public void addServiceNames(Set<String> serviceNames) {
    if (this.serviceNames == null) {
      this.serviceNames = new HashSet<>();
    }

    this.serviceNames.addAll(serviceNames);
  }

  public void addEnvironmentNames(Set<String> environmentNames) {
    if (this.environmentNames == null) {
      this.environmentNames = new HashSet<>();
    }

    this.environmentNames.addAll(environmentNames);
  }
}
