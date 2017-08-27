package software.wings.beans.stats.dashboard.service;

import software.wings.beans.stats.dashboard.ArtifactSummary;
import software.wings.beans.stats.dashboard.EntitySummary;

import java.util.Date;

/**
 * @author rktummala on 08/14/17
 */
public class CurrentActiveInstances {
  private EntitySummary environment;
  private long instanceCount;
  private ArtifactSummary artifact;
  private EntitySummary serviceInfra;
  private Date deployedAt;

  public EntitySummary getEnvironment() {
    return environment;
  }

  public void setEnvironment(EntitySummary environment) {
    this.environment = environment;
  }

  public long getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(long instanceCount) {
    this.instanceCount = instanceCount;
  }

  public ArtifactSummary getArtifact() {
    return artifact;
  }

  public void setArtifact(ArtifactSummary artifact) {
    this.artifact = artifact;
  }

  public EntitySummary getServiceInfra() {
    return serviceInfra;
  }

  public void setServiceInfra(EntitySummary serviceInfra) {
    this.serviceInfra = serviceInfra;
  }

  public Date getDeployedAt() {
    return deployedAt;
  }

  public void setDeployedAt(Date deployedAt) {
    this.deployedAt = deployedAt;
  }

  public static final class Builder {
    private EntitySummary environment;
    private long instanceCount;
    private ArtifactSummary artifact;
    private EntitySummary serviceInfra;
    private Date deployedAt;

    private Builder() {}

    public static Builder aCurrentActiveInstances() {
      return new Builder();
    }

    public Builder withEnvironment(EntitySummary environment) {
      this.environment = environment;
      return this;
    }

    public Builder withInstanceCount(long instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public Builder withArtifact(ArtifactSummary artifact) {
      this.artifact = artifact;
      return this;
    }

    public Builder withServiceInfra(EntitySummary serviceInfra) {
      this.serviceInfra = serviceInfra;
      return this;
    }

    public Builder withDeployedAt(Date deployedAt) {
      this.deployedAt = deployedAt;
      return this;
    }

    public CurrentActiveInstances build() {
      CurrentActiveInstances currentActiveInstances = new CurrentActiveInstances();
      currentActiveInstances.setEnvironment(environment);
      currentActiveInstances.setInstanceCount(instanceCount);
      currentActiveInstances.setArtifact(artifact);
      currentActiveInstances.setServiceInfra(serviceInfra);
      currentActiveInstances.setDeployedAt(deployedAt);
      return currentActiveInstances;
    }
  }
}
