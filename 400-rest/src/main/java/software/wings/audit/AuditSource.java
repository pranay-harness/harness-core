package software.wings.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL) public enum AuditSource { USER, GIT, APIKEY, NONE }
