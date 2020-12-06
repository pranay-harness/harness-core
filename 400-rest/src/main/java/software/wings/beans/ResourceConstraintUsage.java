package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
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
    private String unit;
    private int permits;
    private long acquiredAt;
  }

  List<ActiveScope> activeScopes;
}
