package software.wings.beans.trigger;

import lombok.Builder;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by sgurubelli on 10/25/17.
 */
@Data
@Builder
public class ArtifactSelection {
  private String serviceId;
  private Type type;
  private String artifactStreamId;
  private String artifactFilter;
  private String pipelineId;
  private Set<String> webhookVariables = new HashSet<>();

  public enum Type { ARTIFACT_SOURCE, LAST_COLLECTED, LAST_DEPLOYED, PIPELINE_SOURCE, WEBHOOK_VARIABLE }

  public ArtifactSelection() {}
  public ArtifactSelection(String serviceId, Type type, String artifactStreamId, String artifactFilter,
      String pipelineId, Set<String> webhookVariables) {
    this.serviceId = serviceId;
    this.type = type;
    this.artifactStreamId = artifactStreamId;
    this.artifactFilter = artifactFilter;
    this.pipelineId = pipelineId;
    this.webhookVariables = webhookVariables;
  }
}
