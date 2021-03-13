package io.harness.accesscontrol.acl.mappers;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.HAccessControlDTO;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HACLMapper {
  public static AccessControlDTO toDTO(ACL acl) {
    return HAccessControlDTO.builder().permission(acl.getPermissionIdentifier()).build();
  }
}
