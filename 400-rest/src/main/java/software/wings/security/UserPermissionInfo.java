/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.security.PermissionAttribute.PermissionType;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.dashboard.Action;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class UserPermissionInfo {
  private String accountId;
  private AccountPermissionSummary accountPermissionSummary;
  // Key - appId, Value - app permission summary
  private Map<String, AppPermissionSummaryForUI> appPermissionMap;
  private boolean hasAllAppAccess;

  // Key - appId, Value - app permission summary
  // This structure is optimized for AuthRuleFilter for faster lookup
  @JsonIgnore private Map<String, AppPermissionSummary> appPermissionMapInternal;

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
