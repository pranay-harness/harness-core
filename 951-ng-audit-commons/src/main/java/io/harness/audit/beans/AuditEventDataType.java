package io.harness.audit.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.custom.AuditEventDataTypeConstants;

@OwnedBy(HarnessTeam.PL)
public enum AuditEventDataType {
  UserInvitationAuditEventData(AuditEventDataTypeConstants.USER_INVITATION_AUDIT_EVENT_DATA),
  AddCollaboratorAuditEventData(AuditEventDataTypeConstants.ADD_COLLABORATOR_AUDIT_EVENT_DATA),
  TemplateAuditEventData(AuditEventDataTypeConstants.TEMPLATE_AUDIT_EVENT_DATA),

  // Deprecated
  USER_INVITE(AuditEventDataTypeConstants.USER_INVITE),
  USER_MEMBERSHIP(AuditEventDataTypeConstants.USER_MEMBERSHIP);

  AuditEventDataType(String auditEventDataType) {
    if (!auditEventDataType.equals(this.name())) {
      throw new IllegalArgumentException(
          String.format("AuditEventDataType enum: %s doesn't match constant: %s", this.name(), auditEventDataType));
    }
  }
}
