package io.harness.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public enum Action {
  CREATE,
  UPDATE,
  RESTORE,
  DELETE,
  INVITE,
  RESEND_INVITE,
  REVOKE_INVITE,
  ADD_COLLABORATOR,
  REMOVE_COLLABORATOR,

  // Deprecated
  ADD_MEMBERSHIP,
  REMOVE_MEMBERSHIP
}
