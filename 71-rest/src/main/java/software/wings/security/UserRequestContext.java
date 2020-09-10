package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * @author rktummala on 3/9/18
 */
@OwnedBy(PL)
@Data
@Builder
public class UserRequestContext {
  private String accountId;
  private boolean harnessSupportUser;
  private UserPermissionInfo userPermissionInfo;
  private UserRestrictionInfo userRestrictionInfo;

  private boolean appIdFilterRequired;
  private Set<String> appIds;

  private boolean entityIdFilterRequired;

  // Key - Entity class name   Value - EntityInfo
  private Map<String, EntityInfo> entityInfoMap;

  @Data
  @Builder
  public static class EntityInfo {
    private String entityFieldName;
    private Set<String> entityIds;
  }
}
