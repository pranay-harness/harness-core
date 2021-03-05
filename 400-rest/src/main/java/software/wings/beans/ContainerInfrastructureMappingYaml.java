package software.wings.beans;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class ContainerInfrastructureMappingYaml extends InfrastructureMappingYaml {
  private String cluster;

  public ContainerInfrastructureMappingYaml(String type, String harnessApiVersion, String serviceName,
      String infraMappingType, String deploymentType, String cluster, Map<String, Object> blueprints) {
    super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, blueprints);
    this.cluster = cluster;
  }
}
