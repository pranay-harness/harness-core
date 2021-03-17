package io.harness.accesscontrol.acl.daos;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.clients.PermissionCheckDTO;

import java.util.List;

public interface ACLDAO {
  List<ACL> get(Principal principal, List<PermissionCheckDTO> permissionsRequired);

  ACL save(ACL acl);

  void deleteByPrincipal(Principal principal);
}