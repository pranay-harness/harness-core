package software.wings.beans;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;

import java.util.Optional;

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

  @SchemaIgnore
  @Override
  public String getDisplayName() {
    return String.format("%s(%s/%s::%s)", this.getClusterName(), this.getComputeProviderType(),
        this.getDeploymentType(),
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()));
  }
}
