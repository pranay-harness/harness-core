package io.harness.accesscontrol.acl.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.PermissionCheck;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(PL)
public interface ACLDAO {
  List<Boolean> checkForAccess(Principal principal, List<PermissionCheck> permissionChecks);
}