package software.wings.beans.container;

import software.wings.beans.DeploymentSpecificationYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class EcsServiceSpecificationYaml extends DeploymentSpecificationYaml {
  private String serviceSpecJson;
  private String schedulingStrategy = EcsServiceSpecification.ECS_REPLICA_SCHEDULING_STRATEGY;

  @Builder
  public EcsServiceSpecificationYaml(String type, String harnessApiVersion, String serviceName, String manifestYaml,
      String serviceSpecJson, String schedulingStrategy) {
    super(type, harnessApiVersion);
    this.schedulingStrategy = schedulingStrategy;
    this.serviceSpecJson = serviceSpecJson;
  }
}
