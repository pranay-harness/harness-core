package software.wings.beans.instance.dashboard;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * Artifact information
 * @author rktummala on 08/13/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArtifactSummary extends AbstractEntitySummary {
  private String artifactSourceName;
  private String buildNo;
  private Map<String, Object> artifactParameters;

  @Builder
  public ArtifactSummary(String id, String name, String type, String artifactSourceName, String buildNo,
      Map<String, Object> artifactParameters) {
    super(id, name, type);
    this.artifactSourceName = artifactSourceName;
    this.buildNo = buildNo;
    this.artifactParameters = artifactParameters;
  }
}
