package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.security.PermissionAttribute.PermissionType;

import io.harness.annotations.dev.OwnedBy;
import io.harness.dashboard.Action;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * Created by rktummala on 2/26/18.
 */
@OwnedBy(PL)
@Data
@Builder
public class UserPermissionInfo {
  private String accountId;
  private AccountPermissionSummary accountPermissionSummary;
  // Key - appId, Value - app permission summary
  private Map<String, AppPermissionSummaryForUI> appPermissionMap = new HashMap<>();
  private boolean hasAllAppAccess;

  // Key - appId, Value - app permission summary
  // This structure is optimized for AuthRuleFilter for faster lookup
  @JsonIgnore private Map<String, AppPermissionSummary> appPermissionMapInternal = new HashMap<>();

  @JsonIgnore private Map<String, Set<Action>> dashboardPermissions;

  @JsonIgnore
  public Map<String, AppPermissionSummary> getAppPermissionMapInternal() {
    return appPermissionMapInternal;
  }

  public boolean hasAccountPermission(PermissionType permissionType) {
    return accountPermissionSummary != null && isNotEmpty(accountPermissionSummary.getPermissions())
        && accountPermissionSummary.getPermissions().contains(permissionType);
  }
}
