package io.harness.accesscontrol.roleassignments.privileged;

import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.principals.Principal;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class PrivilegedAccessResult {
  String accountIdentifier;
  Principal principal;
  List<AccessControlDTO> permissionCheckResults;
}
