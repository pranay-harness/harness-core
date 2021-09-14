/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 3/9/18
 */
@OwnedBy(PL)
@Data
@Builder
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
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
