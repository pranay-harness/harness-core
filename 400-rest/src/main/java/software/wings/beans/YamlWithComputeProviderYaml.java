package software.wings.beans;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class YamlWithComputeProviderYaml extends YamlWithComputeProvider {
  private String cluster;

  public YamlWithComputeProviderYaml(String type, String harnessApiVersion, String serviceName, String infraMappingType,
      String deploymentType, String computeProviderType, String computeProviderName, String cluster,
      Map<String, Object> blueprints) {
    super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, computeProviderType,
        computeProviderName, blueprints);
    this.cluster = cluster;
  }
}
