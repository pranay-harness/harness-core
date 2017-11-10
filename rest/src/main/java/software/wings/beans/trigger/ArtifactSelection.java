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

  public enum Type { ARTIFACT_SOURCE, LAST_COLLECTED, LAST_DEPLOYED, PIPELINE_SOURCE, WEBHOOK_VARIABLE }

  public ArtifactSelection() {}

  public ArtifactSelection(String serviceId, String serviceName, Type type, String artifactStreamId,
      String artifactSourceName, String artifactFilter, String pipelineId, String pipelineName) {
    this.serviceId = serviceId;
    this.serviceName = serviceName;
    this.type = type;
    this.artifactStreamId = artifactStreamId;
    this.artifactSourceName = artifactSourceName;
    this.artifactFilter = artifactFilter;
    this.pipelineId = pipelineId;
    this.pipelineName = pipelineName;
  }
}
