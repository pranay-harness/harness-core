package io.harness.accesscontrol.acl.services;

import io.harness.accesscontrol.HPrincipal;
import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.daos.ACLDAO;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.HAccessCheckResponseDTO;
import io.harness.accesscontrol.clients.HAccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class HACLServiceImpl implements ACLService {
  private final ACLDAO aclDAO;

  @Override
  public AccessCheckResponseDTO checkAccess(Principal principal, List<PermissionCheckDTO> permissionCheckDTOList) {
    HPrincipal hPrincipal = (HPrincipal) principal;
    List<ACL> accessControlList = aclDAO.get(hPrincipal, permissionCheckDTOList);
    List<AccessControlDTO> accessControlDTOList = new ArrayList<>();
    for (int i = 0; i < permissionCheckDTOList.size(); i++) {
      PermissionCheckDTO permissionCheckDTO = permissionCheckDTOList.get(i);
      accessControlDTOList.add(HAccessControlDTO.builder()
                                   .permission(permissionCheckDTO.getPermission())
                                   .resourceScope(permissionCheckDTO.getResourceScope())
                                   .resourceIdentifier(permissionCheckDTO.getResourceIdentifier())
                                   .resourceType(permissionCheckDTO.getResourceType())
                                   .accessible(accessControlList.get(i) != null)
                                   .build());
    }
    return HAccessCheckResponseDTO.builder()
        .principal(hPrincipal)
        .accessControlList(accessControlDTOList.stream().map(x -> (HAccessControlDTO) x).collect(Collectors.toList()))
        .build();
  }
}
