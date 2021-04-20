package io.harness.audit.beans.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class AuditEventDataTypeConstants {
  public static final String USER_INVITE = "USER_INVITE";
}
