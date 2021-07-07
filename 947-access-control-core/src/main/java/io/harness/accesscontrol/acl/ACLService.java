package io.harness.accesscontrol.acl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(PL)
public interface ACLService {
  AccessCheckResponseDTO checkAccess(
      @NotNull @Valid Principal principal, @NotNull List<PermissionCheckDTO> permissions);
}
