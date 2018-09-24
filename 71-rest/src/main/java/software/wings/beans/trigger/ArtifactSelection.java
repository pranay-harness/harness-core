package software.wings.beans.trigger;

import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by sgurubelli on 10/25/17.
 */
@Data
@Builder
public class ArtifactSelection {
  @NotEmpty private String serviceId;
  private String serviceName;
  @NotEmpty private Type type;
  private String artifactStreamId;
  private String artifactSourceName;
  private String artifactFilter;
  private String pipelineId;
  private String pipelineName;
  private String workflowId;
  private String workflowName;
  private boolean regex;

  public enum Type { ARTIFACT_SOURCE, LAST_COLLECTED, LAST_DEPLOYED, PIPELINE_SOURCE, WEBHOOK_VARIABLE }

  public void setPipelineId(String pipelineId) {
    this.pipelineId = pipelineId;
    this.workflowId = pipelineId;
  }

  public void setPipelineName(String pipelineName) {
    this.pipelineName = pipelineName;
    this.workflowName = pipelineName;
  }

  public String getPipelineId() {
    if (this.pipelineId == null) {
      return this.workflowId;
    }
    return pipelineId;
  }

  public String getPipelineName() {
    if (this.pipelineName == null) {
      return this.workflowName;
    }
    return this.pipelineName;
  }

  public String getWorkflowId() {
    if (workflowId == null) {
      return pipelineId;
    }
    return workflowId;
  }

  public String getWorkflowName() {
    if (workflowName == null) {
      return pipelineName;
    }
    return workflowName;
  }
}
