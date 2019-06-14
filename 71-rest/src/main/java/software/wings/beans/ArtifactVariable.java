package software.wings.beans;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = true)
public class ArtifactVariable extends Variable {
  private EntityType entityType;
  private String entityId;
  private Map<String, String> displayName;
}
