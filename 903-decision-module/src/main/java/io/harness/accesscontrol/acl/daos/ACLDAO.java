package io.harness.accesscontrol.acl.daos;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.dtos.PermissionCheckDTO;
import io.harness.accesscontrol.acl.models.ACL;

import java.util.List;

public interface ACLDAO {
  List<ACL> get(Principal principal, List<PermissionCheckDTO> permissionsRequired);

  ACL save(ACL acl);
}
