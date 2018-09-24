package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Transient;

import java.util.List;
import java.util.Objects;

public abstract class PhysicalInfrastructureMappingBase extends InfrastructureMapping {
  @Attributes(title = "Host Names", required = true) private List<String> hostNames;
  @Attributes(title = "Load Balancer") private String loadBalancerId;
  @Transient @SchemaIgnore private String loadBalancerName;

  public PhysicalInfrastructureMappingBase(InfrastructureMappingType infrastructureMappingType) {
    super(infrastructureMappingType.name());
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends InfrastructureMapping.YamlWithComputeProvider {
    private List<String> hostNames;
    private String loadBalancer;

    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String name, List<String> hostNames,
        String loadBalancer) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName);
      this.hostNames = hostNames;
      this.loadBalancer = loadBalancer;
    }
  }

  public List<String> getHostNames() {
    return hostNames;
  }

  public void setHostNames(List<String> hostNames) {
    this.hostNames = hostNames;
  }

  @Override
  public String getHostConnectionAttrs() {
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final PhysicalInfrastructureMappingBase other = (PhysicalInfrastructureMappingBase) obj;
    return Objects.equals(this.hostNames, other.hostNames);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("hostNames", hostNames).toString();
  }

  public String getLoadBalancerId() {
    return loadBalancerId;
  }

  public void setLoadBalancerId(String loadBalancerId) {
    this.loadBalancerId = loadBalancerId;
  }

  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }
}
