package io.harness.accesscontrol.acl;

import static io.harness.accesscontrol.permissions.PermissionStatus.EXPERIMENTAL;
import static io.harness.accesscontrol.permissions.PermissionStatus.INACTIVE;
import static io.harness.accesscontrol.permissions.PermissionStatus.STAGING;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.persistence.ACLDAO;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.permissions.Permission;
import io.harness.accesscontrol.permissions.PermissionFilter;
import io.harness.accesscontrol.permissions.PermissionService;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@ValidateOnExecution
@Slf4j
public class ACLServiceImpl implements ACLService {
  private final ACLDAO aclDAO;
  private final Set<String> disabledPermissions;

  @Inject
  public ACLServiceImpl(ACLDAO aclDAO, PermissionService permissionService) {
    this.aclDAO = aclDAO;
    PermissionFilter permissionFilter =
        PermissionFilter.builder().statusFilter(Sets.newHashSet(INACTIVE, EXPERIMENTAL, STAGING)).build();
    disabledPermissions =
        permissionService.list(permissionFilter).stream().map(Permission::getIdentifier).collect(Collectors.toSet());
  }

  private AccessControlDTO getAccessControlDTO(PermissionCheckDTO permissionCheckDTO, boolean permitted) {
    return AccessControlDTO.builder()
        .permission(permissionCheckDTO.getPermission())
        .resourceIdentifier(permissionCheckDTO.getResourceIdentifier())
        .resourceScope(permissionCheckDTO.getResourceScope())
        .resourceType(permissionCheckDTO.getResourceType())
        .permitted(permitted)
        .build();
  }

  @Override
  public AccessCheckResponseDTO checkAccess(Principal principal, List<PermissionCheckDTO> permissionChecks) {
    List<Boolean> allowedAccessList = aclDAO.checkForAccess(principal, permissionChecks);
    List<AccessControlDTO> accessControlDTOList = new ArrayList<>();

    for (int i = 0; i < permissionChecks.size(); i++) {
      PermissionCheckDTO permissionCheckDTO = permissionChecks.get(i);
      if (disabledPermissions.contains(permissionCheckDTO.getPermission())) {
        accessControlDTOList.add(getAccessControlDTO(permissionCheckDTO, true));
      } else {
        accessControlDTOList.add(getAccessControlDTO(permissionCheckDTO, allowedAccessList.get(i)));
      }
    }

    return AccessCheckResponseDTO.builder().principal(principal).accessControlList(accessControlDTOList).build();
  }
}
