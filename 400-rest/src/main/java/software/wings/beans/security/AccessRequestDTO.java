/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.security;

import static io.harness.annotations.dev.HarnessModule._970_RBAC_CORE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
@TargetModule(_970_RBAC_CORE)
public class AccessRequestDTO {
  String accessRequestId;
  String accountId;
  String harnessUserGroupId;
  String harnessUserGroupName;
  Set<String> emailIds;
  long accessStartAt;
  long accessEndAt;
  Integer hours;
  AccessRequest.AccessType accessType;
  boolean accessActive;
}
