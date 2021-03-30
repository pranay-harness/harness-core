package io.harness.accesscontrol.acl.services;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.daos.ACLDAO;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.accesscontrol.preference.services.AccessControlPreferenceService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.AccessDeniedException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class ACLServiceImpl implements ACLService {
  private final ACLDAO aclDAO;
  private final AccessControlPreferenceService accessControlPreferenceService;

  private String validateAndGetAccountIdentifierOrThrow(List<PermissionCheckDTO> permissionCheckDTOList) {
    Map<String, List<ResourceScope>> accountToPermissionsMap =
        permissionCheckDTOList.stream()
            .map(PermissionCheckDTO::getResourceScope)
            .collect(Collectors.groupingBy(ResourceScope::getAccountIdentifier));
    Set<String> accountIdentifiers = accountToPermissionsMap.keySet();
    if (accountIdentifiers.size() > 1) {
      throw new AccessDeniedException(
          "Checking permissions for multiple accounts in a single API call is not allowed", USER);
    }
    return accountIdentifiers.iterator().next();
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
  public AccessCheckResponseDTO checkAccess(Principal principal, List<PermissionCheckDTO> permissionCheckDTOList) {
    String accountIdentifier = validateAndGetAccountIdentifierOrThrow(permissionCheckDTOList);
    if (!accessControlPreferenceService.isAccessControlEnabled(accountIdentifier)) {
      return AccessCheckResponseDTO.builder()
          .accessControlList(permissionCheckDTOList.stream()
                                 .map(permissionCheckDTO -> getAccessControlDTO(permissionCheckDTO, true))
                                 .collect(Collectors.toList()))
          .principal(principal)
          .build();
    }

    List<ACL> accessControlList = aclDAO.get(principal, permissionCheckDTOList);
    List<AccessControlDTO> accessControlDTOList = new ArrayList<>();
    for (int i = 0; i < permissionCheckDTOList.size(); i++) {
      PermissionCheckDTO permissionCheckDTO = permissionCheckDTOList.get(i);
      accessControlDTOList.add(getAccessControlDTO(permissionCheckDTO, accessControlList.get(i) != null));
    }
    return AccessCheckResponseDTO.builder().principal(principal).accessControlList(accessControlDTOList).build();
  }

  @Override
  public long insertAllIgnoringDuplicates(List<ACL> acls) {
    return aclDAO.insertAllIgnoringDuplicates(acls);
  }

  @Override
  public long saveAll(List<ACL> acls) {
    return aclDAO.saveAll(acls);
  }

  @Override
  public void deleteAll(List<ACL> acls) {
    aclDAO.deleteAll(acls);
  }

  @Override
  public long deleteByRoleAssignmentId(String roleAssignmentId) {
    return aclDAO.deleteByRoleAssignmentId(roleAssignmentId);
  }

  @Override
  public List<ACL> getByUserGroup(String scopeIdentifier, String userGroupIdentifier) {
    return aclDAO.getByUserGroup(scopeIdentifier, userGroupIdentifier);
  }

  @Override
  public List<ACL> getByRole(String scopeIdentifier, String identifier, boolean managed) {
    return aclDAO.getByRole(scopeIdentifier, identifier, managed);
  }

  @Override
  public List<ACL> getByResourceGroup(String scopeIdentifier, String identifier, boolean managed) {
    return aclDAO.getByResourceGroup(scopeIdentifier, identifier, managed);
  }

  @Override
  public List<ACL> getByRoleAssignmentId(String id) {
    return aclDAO.getByRoleAssignmentId(id);
  }
}
