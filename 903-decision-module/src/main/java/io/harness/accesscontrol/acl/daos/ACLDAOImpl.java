package io.harness.accesscontrol.acl.daos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.repository.ACLRepository;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeParams;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

@OwnedBy(PL)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class ACLDAOImpl implements ACLDAO {
  private static final String PATH_DELIMITER = "/";
  private static final String ALL_RESOURCES_IDENTIFIER = "*";
  private final ACLRepository aclRepository;
  private final ScopeService scopeService;

  private String getResourceSelector(String resourceType, String resourceIdentifier) {
    return PATH_DELIMITER.concat(resourceType).concat(PATH_DELIMITER).concat(resourceIdentifier);
  }

  private List<ACL> processACLQueries(List<PermissionCheckDTO> permissionsRequired, Principal principal) {
    List<ACL> aclList = new ArrayList<>();
    permissionsRequired.forEach(permissionCheckDTO -> {
      ScopeParams scopeParams = HarnessScopeParams.builder()
                                    .accountIdentifier(permissionCheckDTO.getResourceScope().getAccountIdentifier())
                                    .orgIdentifier(permissionCheckDTO.getResourceScope().getOrgIdentifier())
                                    .projectIdentifier(permissionCheckDTO.getResourceScope().getProjectIdentifier())
                                    .build();
      Scope scope = scopeService.buildScopeFromParams(scopeParams);

      String queryStringForResource = ACL.getAclQueryString(scope.toString(),
          getResourceSelector(permissionCheckDTO.getResourceType(), permissionCheckDTO.getResourceIdentifier()),
          principal.getPrincipalType().name(), principal.getPrincipalIdentifier(), permissionCheckDTO.getPermission());

      String queryStringForAllResources = ACL.getAclQueryString(scope.toString(),
          getResourceSelector(permissionCheckDTO.getResourceType(), ALL_RESOURCES_IDENTIFIER),
          principal.getPrincipalType().name(), principal.getPrincipalIdentifier(), permissionCheckDTO.getPermission());

      String queryStringForParentResource = ACL.getAclQueryString(scope.getParentScope().toString(),
          getResourceSelector(scope.getLevel().getResourceType(), scope.getInstanceId()),
          principal.getPrincipalType().name(), principal.getPrincipalIdentifier(), permissionCheckDTO.getPermission());

      List<ACL> aclsInDB = aclRepository.getByAclQueryStringInAndEnabled(
          Lists.newArrayList(queryStringForResource, queryStringForAllResources, queryStringForParentResource), true);

      if (!aclsInDB.isEmpty()) {
        aclList.add(aclsInDB.get(0));
      } else {
        aclList.add(null);
      }
    });
    return aclList;
  }

  @Override
  public List<ACL> get(Principal principal, List<PermissionCheckDTO> permissionsRequired) {
    return processACLQueries(permissionsRequired, principal);
  }

  @Override
  public ACL save(ACL acl) {
    return aclRepository.save(acl);
  }

  @Override
  public long insertAllIgnoringDuplicates(List<ACL> acls) {
    try {
      return aclRepository.insertAllIgnoringDuplicates(acls);
    } catch (DuplicateKeyException duplicateKeyException) {
      return 0;
    }
  }

  @Override
  public long saveAll(List<ACL> acls) {
    return aclRepository.saveAll(acls).spliterator().estimateSize();
  }

  @Override
  public void deleteAll(List<ACL> acls) {
    aclRepository.deleteAll(acls);
  }

  @Override
  public long deleteByRoleAssignmentId(String roleAssignmentId) {
    return aclRepository.deleteByRoleAssignmentId(roleAssignmentId);
  }

  @Override
  public List<ACL> getByUserGroup(String scopeIdentifier, String userGroupIdentifier) {
    return aclRepository.findByUserGroup(scopeIdentifier, userGroupIdentifier);
  }

  @Override
  public List<ACL> getByRole(String scopeIdentifier, String identifier, boolean managed) {
    return aclRepository.findByRole(scopeIdentifier, identifier, managed);
  }

  @Override
  public List<ACL> getByResourceGroup(String scopeIdentifier, String identifier, boolean managed) {
    return aclRepository.findByResourceGroup(scopeIdentifier, identifier, managed);
  }

  @Override
  public List<ACL> getByRoleAssignmentId(String id) {
    return aclRepository.getByRoleAssignmentId(id);
  }
}
