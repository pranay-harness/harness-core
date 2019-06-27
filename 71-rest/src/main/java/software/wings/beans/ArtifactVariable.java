package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.artifact.ArtifactStreamSummary;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ArtifactVariable extends Variable {
  private EntityType entityType;
  private String entityId;
  private List<ArtifactVariable> overriddenArtifactVariables;
  private List<ArtifactStreamSummary> artifactStreamSummaries;
  private Map<String, List<String>> displayInfo;

  @Builder
  public ArtifactVariable(String name, String description, boolean mandatory, String value, boolean fixed,
      String allowedValues, List<String> allowedList, Map<String, Object> metadata, VariableType type,
      EntityType entityType, String entityId, List<ArtifactVariable> overriddenArtifactVariables,
      List<ArtifactStreamSummary> artifactStreamSummaries, Map<String, List<String>> displayInfo) {
    super(name, description, mandatory, value, fixed, allowedValues, allowedList, metadata, type);
    this.entityType = entityType;
    this.entityId = entityId;
    this.overriddenArtifactVariables = overriddenArtifactVariables;
    this.artifactStreamSummaries = artifactStreamSummaries;
    this.displayInfo = displayInfo;
  }
}
