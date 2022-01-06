/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

@Scope(PermissionAttribute.ResourceType.PIPELINE)
@OwnedBy(CDC)
public interface QLApprovalDetails {
  String getApprovalId();
  ApprovalStateType getApprovalType();
  String getApprovalName();
  String getStageName();
  String getStepName();
  Long getStartedAt();
  Long getWillExpireAt();
  EmbeddedUser getTriggeredBy();
}
