package io.harness.accesscontrol.acl.mappers;

import io.harness.accesscontrol.acl.dtos.AccessControlDTO;
import io.harness.accesscontrol.acl.dtos.HAccessControlDTO;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.models.HACL;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HACLMapper {
  public static AccessControlDTO toDTO(ACL acl) {
    HACL hacl = (HACL) acl;
    return HAccessControlDTO.builder().permission(hacl.getPermission()).build();
  }
}
