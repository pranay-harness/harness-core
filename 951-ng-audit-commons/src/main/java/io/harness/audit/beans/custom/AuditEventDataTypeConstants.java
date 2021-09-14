/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.audit.beans.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class AuditEventDataTypeConstants {
  public static final String USER_INVITATION_AUDIT_EVENT_DATA = "UserInvitationAuditEventData";
  public static final String ADD_COLLABORATOR_AUDIT_EVENT_DATA = "AddCollaboratorAuditEventData";

  // Deprecated
  public static final String USER_INVITE = "USER_INVITE";
  public static final String USER_MEMBERSHIP = "USER_MEMBERSHIP";
}
