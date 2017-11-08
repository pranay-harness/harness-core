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
  @NotEmpty private Type type;
  private String artifactStreamId;
  private String artifactFilter;
  private String pipelineId;

  public enum Type { ARTIFACT_SOURCE, LAST_COLLECTED, LAST_DEPLOYED, PIPELINE_SOURCE, WEBHOOK_VARIABLE }

  public ArtifactSelection() {}

  public ArtifactSelection(
      String serviceId, Type type, String artifactStreamId, String artifactFilter, String pipelineId) {
    this.serviceId = serviceId;
    this.type = type;
    this.artifactStreamId = artifactStreamId;
    this.artifactFilter = artifactFilter;
    this.pipelineId = pipelineId;
  }
}
