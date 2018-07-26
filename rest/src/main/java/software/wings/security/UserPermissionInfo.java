package software.wings.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import software.wings.settings.UsageRestrictions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by rktummala on 2/26/18.
 */
@Builder
@Data
public class UserPermissionInfo {
  private String accountId;
  private boolean isRbacEnabled;
  private AccountPermissionSummary accountPermissionSummary;
  // Key - appId, Value - app permission summary
  private Map<String, AppPermissionSummaryForUI> appPermissionMap = new HashMap<>();

  private UsageRestrictions usageRestrictions;

  private Map<String, Set<String>> appEnvMap;

  // Key - appId, Value - app permission summary
  // This structure is optimized for AuthRuleFilter for faster lookup
  @JsonIgnore private Map<String, AppPermissionSummary> appPermissionMapInternal = new HashMap<>();

  @JsonIgnore
  public Map<String, AppPermissionSummary> getAppPermissionMapInternal() {
    return appPermissionMapInternal;
  }
}
