package software.wings.beans.stats.dashboard.service;

import software.wings.beans.stats.dashboard.ArtifactSummary;
import software.wings.beans.stats.dashboard.EntitySummary;

import java.util.Date;
import java.util.List;

/**
 * @author rktummala on 08/14/17
 */
public class PipelineExecutionHistory {
  private EntitySummary pipeline;
  private ArtifactSummary artifact;
  private String status;
  private Date startTime;
  private Date endTime;
  private List<EntitySummary> environmentList;

  public EntitySummary getPipeline() {
    return pipeline;
  }

  private void setPipeline(EntitySummary pipeline) {
    this.pipeline = pipeline;
  }

  public ArtifactSummary getArtifact() {
    return artifact;
  }

  private void setArtifact(ArtifactSummary artifact) {
    this.artifact = artifact;
  }

  public String getStatus() {
    return status;
  }

  private void setStatus(String status) {
    this.status = status;
  }

  public Date getStartTime() {
    return startTime;
  }

  private void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  private void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  public List<EntitySummary> getEnvironmentList() {
    return environmentList;
  }

  private void setEnvironmentList(List<EntitySummary> environmentList) {
    this.environmentList = environmentList;
  }

  public static final class Builder {
    private EntitySummary pipeline;
    private ArtifactSummary artifact;
    private String status;
    private Date startTime;
    private Date endTime;
    private List<EntitySummary> environmentList;

    private Builder() {}

    public static Builder aPipelineExecutionHistory() {
      return new Builder();
    }

    public Builder withPipeline(EntitySummary pipeline) {
      this.pipeline = pipeline;
      return this;
    }

    public Builder withArtifact(ArtifactSummary artifact) {
      this.artifact = artifact;
      return this;
    }

    public Builder withStatus(String status) {
      this.status = status;
      return this;
    }

    public Builder withStartTime(Date startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder withEndTime(Date endTime) {
      this.endTime = endTime;
      return this;
    }

    public Builder withEnvironmentList(List<EntitySummary> environmentList) {
      this.environmentList = environmentList;
      return this;
    }

    public PipelineExecutionHistory build() {
      PipelineExecutionHistory pipelineExecutionHistory = new PipelineExecutionHistory();
      pipelineExecutionHistory.setPipeline(pipeline);
      pipelineExecutionHistory.setArtifact(artifact);
      pipelineExecutionHistory.setStatus(status);
      pipelineExecutionHistory.setStartTime(startTime);
      pipelineExecutionHistory.setEndTime(endTime);
      pipelineExecutionHistory.setEnvironmentList(environmentList);
      return pipelineExecutionHistory;
    }
  }
}
