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
@Data
@Builder
public class UserPermissionInfo {
  private String accountId;
  private AccountPermissionSummary accountPermissionSummary;
  // Key - appId, Value - app permission summary
  private Map<String, AppPermissionSummaryForUI> appPermissionMap = new HashMap<>();

  @JsonIgnore private UsageRestrictions usageRestrictionsForUpdateAction;
  @JsonIgnore private Map<String, Set<String>> appEnvMapForUpdateAction;

  @JsonIgnore private UsageRestrictions usageRestrictionsForReadAction;
  @JsonIgnore private Map<String, Set<String>> appEnvMapForReadAction;

  // Key - appId, Value - app permission summary
  // This structure is optimized for AuthRuleFilter for faster lookup
  @JsonIgnore private Map<String, AppPermissionSummary> appPermissionMapInternal = new HashMap<>();

  @JsonIgnore
  public Map<String, AppPermissionSummary> getAppPermissionMapInternal() {
    return appPermissionMapInternal;
  }

  @JsonIgnore
  public UsageRestrictions getUsageRestrictionsForUpdateAction() {
    return usageRestrictionsForUpdateAction;
  }

  @JsonIgnore
  public Map<String, Set<String>> getAppEnvMapForUpdateAction() {
    return appEnvMapForUpdateAction;
  }

  @JsonIgnore
  public UsageRestrictions getUsageRestrictionsForReadAction() {
    return usageRestrictionsForReadAction;
  }

  @JsonIgnore
  public Map<String, Set<String>> getAppEnvMapForReadAction() {
    return appEnvMapForReadAction;
  }
}
