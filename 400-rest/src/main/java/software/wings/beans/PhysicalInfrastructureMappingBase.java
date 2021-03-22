package software.wings.beans;

import software.wings.beans.infrastructure.Host;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Objects;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Transient;

@FieldNameConstants(innerTypeName = "PhysicalInfrastructureMappingBaseKeys")
public abstract class PhysicalInfrastructureMappingBase extends InfrastructureMapping {
  @Attributes(title = "Host Names", required = true) private List<String> hostNames;
  private List<Host> hosts;
  @Attributes(title = "Load Balancer") private String loadBalancerId;
  @Transient @SchemaIgnore private String loadBalancerName;

  public PhysicalInfrastructureMappingBase(InfrastructureMappingType infrastructureMappingType) {
    super(infrastructureMappingType.name());
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

  public List<Host> hosts() {
    return hosts;
  }

  public void hosts(List<Host> hosts) {
    this.hosts = hosts;
  }
}
