package software.wings.beans.instance.dashboard;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Artifact information
 * @author rktummala on 08/13/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArtifactSummary extends AbstractEntitySummary {
  private String artifactSourceName;
  private String buildNo;

  @Builder
  public ArtifactSummary(String id, String name, String type, String artifactSourceName, String buildNo) {
    super(id, name, type);
    this.artifactSourceName = artifactSourceName;
    this.buildNo = buildNo;
  }
}
