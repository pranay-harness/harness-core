package io.harness.accesscontrol.acl.services;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.daos.ACLDAO;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class ACLServiceImpl implements ACLService {
  private final ACLDAO aclDAO;

  @Override
  public AccessCheckResponseDTO checkAccess(Principal principal, List<PermissionCheckDTO> permissionCheckDTOList) {
    List<ACL> accessControlList = aclDAO.get(principal, permissionCheckDTOList);
    List<AccessControlDTO> accessControlDTOList = new ArrayList<>();
    for (int i = 0; i < permissionCheckDTOList.size(); i++) {
      PermissionCheckDTO permissionCheckDTO = permissionCheckDTOList.get(i);
      accessControlDTOList.add(AccessControlDTO.builder()
                                   .permission(permissionCheckDTO.getPermission())
                                   .resourceScope(permissionCheckDTO.getResourceScope())
                                   .resourceIdentifier(permissionCheckDTO.getResourceIdentifier())
                                   .resourceType(permissionCheckDTO.getResourceType())
                                   .permitted(accessControlList.get(i) != null)
                                   .build());
    }
    return AccessCheckResponseDTO.builder().principal(principal).accessControlList(accessControlDTOList).build();
  }

  @Override
  public AccessCheckResponseDTO checkAccess(
      String principalType, String principalIdentifier, List<PermissionCheckDTO> permissionCheckDTOList) {
    return checkAccess(Principal.builder()
                           .principalType(PrincipalType.valueOf(principalType))
                           .principalIdentifier(principalIdentifier)
                           .build(),
        permissionCheckDTOList);
  }

  @Override
  public List<ACL> get(Principal principal, List<PermissionCheckDTO> permissionsRequired) {
    return aclDAO.get(principal, permissionsRequired);
  }

  @Override
  public ACL save(ACL acl) {
    return aclDAO.save(acl);
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
