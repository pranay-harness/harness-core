package io.harness.accesscontrol.acl.services;

import io.harness.accesscontrol.acl.daos.ACLDAO;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.clients.AccessCheckRequestDTO;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.HAccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.aggregator.services.apis.ACLAggregatorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class HACLServiceImpl implements ACLService {
  private final ACLAggregatorService aclAggregatorService;
  private final ACLDAO aclDAO;

  @Override
  public AccessCheckResponseDTO get(AccessCheckRequestDTO dto) {
    boolean success = aclAggregatorService.aggregate(dto.getPrincipal());
    log.info(
        "Call to aggregator service to refresh permissions for principal {} returned: {}", dto.getPrincipal(), success);

    List<ACL> accessControlList = aclDAO.get(dto.getPrincipal(), dto.getPermissions());
    List<AccessControlDTO> accessControlDTOList = new ArrayList<>();
    for (int i = 0; i < dto.getPermissions().size(); i++) {
      PermissionCheckDTO permissionCheckDTO = dto.getPermissions().get(i);
      accessControlDTOList.add(HAccessControlDTO.builder()
                                   .permission(permissionCheckDTO.getPermission())
                                   .accountIdentifier(permissionCheckDTO.getAccountIdentifier())
                                   .orgIdentifier(permissionCheckDTO.getOrgIdentifier())
                                   .projectIdentifier(permissionCheckDTO.getProjectIdentifier())
                                   .resourceIdentifier(permissionCheckDTO.getResourceIdentifier())
                                   .resourceType(permissionCheckDTO.getResourceType())
                                   .accessible(accessControlList.get(i) != null)
                                   .build());
    }
    return AccessCheckResponseDTO.builder()
        .principal(dto.getPrincipal())
        .accessControlList(accessControlDTOList)
        .build();
  }
}
