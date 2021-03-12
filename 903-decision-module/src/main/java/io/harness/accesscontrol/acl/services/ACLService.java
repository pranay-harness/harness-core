package io.harness.accesscontrol.acl.services;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;

import java.util.List;

public interface ACLService {
  AccessCheckResponseDTO checkAccess(Principal principal, List<PermissionCheckDTO> permissionCheckDTOList);
}
