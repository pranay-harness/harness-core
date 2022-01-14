package io.harness.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public enum Action {
  CREATE,
  UPDATE,
  RESTORE,
  DELETE,
  UPSERT,
  INVITE,
  RESEND_INVITE,
  REVOKE_INVITE,
  ADD_COLLABORATOR,
  REMOVE_COLLABORATOR,
  REVOKE_TOKEN,

  // Deprecated
  ADD_MEMBERSHIP,
  REMOVE_MEMBERSHIP
}
