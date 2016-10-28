package software.wings.api;

import software.wings.sm.StateExecutionData;

/**
 * Created by anubhaw on 10/26/16.
 */
public class BuildStateExecutionData extends StateExecutionData {
  private String artifactStreamId;

  public String getArtifactStreamId() {
    return artifactStreamId;
  }

  public void setArtifactStreamId(String artifactStreamId) {
    this.artifactStreamId = artifactStreamId;
  }
}
