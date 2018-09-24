package software.wings.beans;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by rishi on 5/18/17.
 */
public abstract class ContainerInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Cluster Name") private String clusterName;

  /**
   * Instantiates a new Infrastructure mapping.
   *
   * @param infraMappingType the infra mapping type
   */
  public ContainerInfrastructureMapping(String infraMappingType) {
    super(infraMappingType);
  }

  /**
   * Gets cluster name.
   *
   * @return the cluster name
   */
  @Attributes(title = "Cluster Name")
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Sets cluster name.
   *
   * @param clusterName the cluster name
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return null;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public abstract static class Yaml extends InfrastructureMapping.Yaml {
    private String cluster;

    public Yaml(String type, String harnessApiVersion, String serviceName, String infraMappingType,
        String deploymentType, String cluster) {
      super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType);
      this.cluster = cluster;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public abstract static class YamlWithComputeProvider extends InfrastructureMapping.YamlWithComputeProvider {
    private String cluster;

    public YamlWithComputeProvider(String type, String harnessApiVersion, String serviceName, String infraMappingType,
        String deploymentType, String computeProviderType, String computeProviderName, String cluster) {
      super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, computeProviderType,
          computeProviderName);
      this.cluster = cluster;
    }
  }
  public abstract String getNamespace();
}
