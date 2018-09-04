package software.wings.beans;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ResourceConstraintUsage {
  String resourceConstraintId;

  @Value
  @Builder
  public static class ActiveScope {
    private String releaseEntityType;
    private String releaseEntityId;
    private String releaseEntityName;
    private int permits;
    private long acquiredAt;
  }

  List<ActiveScope> activeScopes;
}
