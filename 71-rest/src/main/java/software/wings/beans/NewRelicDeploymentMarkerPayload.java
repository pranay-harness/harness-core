package software.wings.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewRelicDeploymentMarkerPayload {
  private Deployment deployment;

  @Data
  @Builder
  public static class Deployment {
    private String revision;
    private String description;
    private String changelog;
    private String user;
  }
}
