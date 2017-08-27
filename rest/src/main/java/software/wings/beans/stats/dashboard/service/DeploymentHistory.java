package software.wings.beans.stats.dashboard.service;

import software.wings.beans.stats.dashboard.ArtifactSummary;
import software.wings.beans.stats.dashboard.EntitySummary;

import java.util.Date;

/**
 * @author rktummala on 08/14/17
 */
public class DeploymentHistory {
  private ArtifactSummary artifact;
  private Date deployedAt;
  private String status;
  private EntitySummary triggeredBy;
  private EntitySummary pipeline;
  private EntitySummary workflow;
  private long instanceCount;
  private EntitySummary serviceInfra;

  public ArtifactSummary getArtifact() {
    return artifact;
  }

  private void setArtifact(ArtifactSummary artifact) {
    this.artifact = artifact;
  }

  public Date getDeployedAt() {
    return deployedAt;
  }

  private void setDeployedAt(Date deployedAt) {
    this.deployedAt = deployedAt;
  }

  public String getStatus() {
    return status;
  }

  private void setStatus(String status) {
    this.status = status;
  }

  public EntitySummary getTriggeredBy() {
    return triggeredBy;
  }

  private void setTriggeredBy(EntitySummary triggeredBy) {
    this.triggeredBy = triggeredBy;
  }

  public EntitySummary getPipeline() {
    return pipeline;
  }

  private void setPipeline(EntitySummary pipeline) {
    this.pipeline = pipeline;
  }

  public EntitySummary getWorkflow() {
    return workflow;
  }

  private void setWorkflow(EntitySummary workflow) {
    this.workflow = workflow;
  }

  public long getInstanceCount() {
    return instanceCount;
  }

  private void setInstanceCount(long instanceCount) {
    this.instanceCount = instanceCount;
  }

  public EntitySummary getServiceInfra() {
    return serviceInfra;
  }

  private void setServiceInfra(EntitySummary serviceInfra) {
    this.serviceInfra = serviceInfra;
  }

  public static final class Builder {
    private ArtifactSummary artifact;
    private Date deployedAt;
    private String status;
    private EntitySummary triggeredBy;
    private EntitySummary pipeline;
    private EntitySummary workflow;
    private long instanceCount;
    private EntitySummary serviceInfra;

    private Builder() {}

    public static Builder aDeploymentHistory() {
      return new Builder();
    }

    public Builder withArtifact(ArtifactSummary artifact) {
      this.artifact = artifact;
      return this;
    }

    public Builder withDeployedAt(Date deployedAt) {
      this.deployedAt = deployedAt;
      return this;
    }

    public Builder withStatus(String status) {
      this.status = status;
      return this;
    }

    public Builder withTriggeredBy(EntitySummary triggeredBy) {
      this.triggeredBy = triggeredBy;
      return this;
    }

    public Builder withPipeline(EntitySummary pipeline) {
      this.pipeline = pipeline;
      return this;
    }

    public Builder withWorkflow(EntitySummary workflow) {
      this.workflow = workflow;
      return this;
    }

    public Builder withInstanceCount(long instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public Builder withServiceInfra(EntitySummary serviceInfra) {
      this.serviceInfra = serviceInfra;
      return this;
    }

    public DeploymentHistory build() {
      DeploymentHistory deploymentHistory = new DeploymentHistory();
      deploymentHistory.setArtifact(artifact);
      deploymentHistory.setDeployedAt(deployedAt);
      deploymentHistory.setStatus(status);
      deploymentHistory.setTriggeredBy(triggeredBy);
      deploymentHistory.setPipeline(pipeline);
      deploymentHistory.setWorkflow(workflow);
      deploymentHistory.setInstanceCount(instanceCount);
      deploymentHistory.setServiceInfra(serviceInfra);
      return deploymentHistory;
    }
  }
}
