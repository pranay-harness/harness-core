package io.harness.accesscontrol.acl.services;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.clients.AccessCheckRequestDTO;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public interface ACLService {
  AccessCheckResponseDTO checkAccess(
      io.harness.security.dto.Principal contextPrincipal, AccessCheckRequestDTO accessCheckRequestDTO);
}
