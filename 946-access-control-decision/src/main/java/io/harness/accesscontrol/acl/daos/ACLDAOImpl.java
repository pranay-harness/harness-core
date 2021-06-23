package io.harness.accesscontrol.acl.daos;

import static io.harness.accesscontrol.acl.models.ACL.getAclQueryString;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.repository.ACLRepository;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(PL)
@Singleton
@Slf4j
public class ACLDAOImpl implements ACLDAO {
  private static final String PATH_DELIMITER = "/";
  private final ACLRepository aclRepository;
  private final ScopeService scopeService;
  private final Set<String> resourceTypes;

  @Inject
  public ACLDAOImpl(@Named(ACL.PRIMARY_COLLECTION) ACLRepository aclRepository, ScopeService scopeService,
      Map<String, ScopeLevel> scopeLevels) {
    this.aclRepository = aclRepository;
    this.scopeService = scopeService;
    this.resourceTypes = scopeLevels.values().stream().map(ScopeLevel::getResourceType).collect(Collectors.toSet());
  }

  private String getResourceSelector(String resourceType, String resourceIdentifier) {
    return PATH_DELIMITER.concat(resourceType).concat(PATH_DELIMITER).concat(resourceIdentifier);
  }

  private String getScope(ResourceScope resourceScope) {
    if (resourceScope != null && !StringUtils.isEmpty(resourceScope.getAccountIdentifier())) {
      return scopeService
          .buildScopeFromParams(HarnessScopeParams.builder()
                                    .accountIdentifier(resourceScope.getAccountIdentifier())
                                    .orgIdentifier(resourceScope.getOrgIdentifier())
                                    .projectIdentifier(resourceScope.getProjectIdentifier())
                                    .build())
          .toString();
    }
    return "";
  }

  private List<String> getQueryStrings(PermissionCheckDTO permissionCheckDTO, Principal principal) {
    String scope = getScope(permissionCheckDTO.getResourceScope());
    List<String> queryStrings = new ArrayList<>();

    // query for resource=/RESOURCE_TYPE/{resourceIdentifier} in given scope
    if (!StringUtils.isEmpty(permissionCheckDTO.getResourceIdentifier())) {
      queryStrings.add(getAclQueryString(scope,
          getResourceSelector(permissionCheckDTO.getResourceType(), permissionCheckDTO.getResourceIdentifier()),
          principal.getPrincipalType().name(), principal.getPrincipalIdentifier(), permissionCheckDTO.getPermission()));
    }

    // query for resource=/RESOURCE_TYPE/* in given scope
    queryStrings.add(getAclQueryString(scope, getResourceSelector(permissionCheckDTO.getResourceType(), "*"),
        principal.getPrincipalType().name(), principal.getPrincipalIdentifier(), permissionCheckDTO.getPermission()));

    // query for resource=/*/* in given scope
    queryStrings.add(getAclQueryString(scope, getResourceSelector("*", "*"), principal.getPrincipalType().name(),
        principal.getPrincipalIdentifier(), permissionCheckDTO.getPermission()));

    // if RESOURCE_TYPE is a scope, query for scope = {given_scope}/RESOURCE_TYPE/{resourceIdentifier}
    if (resourceTypes.contains(permissionCheckDTO.getResourceType())) {
      scope = scope.concat(PATH_DELIMITER + permissionCheckDTO.getResourceType() + PATH_DELIMITER
          + permissionCheckDTO.getResourceIdentifier());

      // and resource = /RESOURCE_TYPE/{resourceIdentifier}
      if (!StringUtils.isEmpty(permissionCheckDTO.getResourceIdentifier())) {
        queryStrings.add(getAclQueryString(scope,
            getResourceSelector(permissionCheckDTO.getResourceType(), permissionCheckDTO.getResourceIdentifier()),
            principal.getPrincipalType().name(), principal.getPrincipalIdentifier(),
            permissionCheckDTO.getPermission()));
      }

      // and resource = /RESOURCE_TYPE/*
      queryStrings.add(getAclQueryString(scope, getResourceSelector(permissionCheckDTO.getResourceType(), "*"),
          principal.getPrincipalType().name(), principal.getPrincipalIdentifier(), permissionCheckDTO.getPermission()));

      // and resource = /*/*
      queryStrings.add(getAclQueryString(scope, getResourceSelector("*", "*"), principal.getPrincipalType().name(),
          principal.getPrincipalIdentifier(), permissionCheckDTO.getPermission()));
    }
    return queryStrings;
  }

  @Override
  public List<Boolean> checkForAccess(Principal principal, List<PermissionCheckDTO> permissionsRequired) {
    List<List<String>> aclQueryStringsPerPermission = new ArrayList<>();
    List<String> aclQueryStrings = new ArrayList<>();
    permissionsRequired.forEach(permissionCheckDTO -> {
      List<String> queryStrings = getQueryStrings(permissionCheckDTO, principal);
      aclQueryStringsPerPermission.add(queryStrings);
      aclQueryStrings.addAll(queryStrings);
    });

    Set<String> aclsPresentInDB = aclRepository.getByAclQueryStringInAndEnabled(aclQueryStrings, true);
    return aclQueryStringsPerPermission.stream()
        .map(queryStringsForPermission -> queryStringsForPermission.stream().anyMatch(aclsPresentInDB::contains))
        .collect(Collectors.toList());
  }

  @Override
  public long saveAll(List<ACL> acls) {
    return aclRepository.insertAllIgnoringDuplicates(acls);
  }

  @Override
  public long deleteByRoleAssignment(String roleAssignmentId) {
    return aclRepository.deleteByRoleAssignmentId(roleAssignmentId);
  }

  @Override
  public List<ACL> getByUserGroup(String scope, String userGroupIdentifier) {
    return aclRepository.findByUserGroup(scope, userGroupIdentifier);
  }

  @Override
  public List<ACL> getByRole(String scope, String roleIdentifier, boolean managed) {
    return aclRepository.findByRole(scope, roleIdentifier, managed);
  }

  @Override
  public List<ACL> getByResourceGroup(String scope, String resourceGroupIdentifier, boolean managed) {
    return aclRepository.findByResourceGroup(scope, resourceGroupIdentifier, managed);
  }

  @Override
  public List<ACL> getByRoleAssignment(String roleAssignmentId) {
    return aclRepository.getByRoleAssignmentId(roleAssignmentId);
  }
}
