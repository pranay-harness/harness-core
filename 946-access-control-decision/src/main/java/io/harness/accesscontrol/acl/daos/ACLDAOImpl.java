package io.harness.accesscontrol.acl.daos;

import static io.harness.accesscontrol.acl.models.ACL.getAclQueryString;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.repository.ACLRepository;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;

@OwnedBy(PL)
@Singleton
@Slf4j
public class ACLDAOImpl implements ACLDAO {
  private static final String PATH_DELIMITER = "/";
  private static final String ALL_RESOURCES_IDENTIFIER = "*";
  private final ACLRepository aclRepository;
  private final ScopeService scopeService;
  private final Set<String> resourceTypes;

  @Inject
  public ACLDAOImpl(ACLRepository aclRepository, ScopeService scopeService, Map<String, ScopeLevel> scopeLevels) {
    this.aclRepository = aclRepository;
    this.scopeService = scopeService;
    this.resourceTypes = scopeLevels.values().stream().map(ScopeLevel::getResourceType).collect(Collectors.toSet());
  }

  private String getResourceSelector(String resourceType, String resourceIdentifier) {
    return PATH_DELIMITER.concat(resourceType).concat(PATH_DELIMITER).concat(resourceIdentifier);
  }

  private Optional<Scope> getScope(ResourceScope resourceScope) {
    if (Optional.ofNullable(resourceScope)
            .map(ResourceScope::getAccountIdentifier)
            .filter(x -> !StringUtils.isEmpty(x))
            .isPresent()) {
      return Optional.of(scopeService.buildScopeFromParams(HarnessScopeParams.builder()
                                                               .accountIdentifier(resourceScope.getAccountIdentifier())
                                                               .orgIdentifier(resourceScope.getOrgIdentifier())
                                                               .projectIdentifier(resourceScope.getProjectIdentifier())
                                                               .build()));
    }
    return Optional.empty();
  }

  private List<ACL> processACLQueries(List<PermissionCheckDTO> permissionsRequired, Principal principal) {
    List<ACL> aclList = new ArrayList<>();
    permissionsRequired.forEach(permissionCheckDTO -> {
      List<String> queryStrings = new ArrayList<>();
      Optional<Scope> scopeOptional = getScope(permissionCheckDTO.getResourceScope());

      if (scopeOptional.isPresent()) {
        Scope scope = scopeOptional.get();
        queryStrings.add(getAclQueryString(scope.toString(),
            getResourceSelector(permissionCheckDTO.getResourceType(), permissionCheckDTO.getResourceIdentifier()),
            principal.getPrincipalType().name(), principal.getPrincipalIdentifier(),
            permissionCheckDTO.getPermission()));

        queryStrings.add(getAclQueryString(scope.toString(),
            getResourceSelector(permissionCheckDTO.getResourceType(), ALL_RESOURCES_IDENTIFIER),
            principal.getPrincipalType().name(), principal.getPrincipalIdentifier(),
            permissionCheckDTO.getPermission()));

        String currentResourceType = permissionCheckDTO.getResourceType();
        Scope currentScope = scope;
        while (currentScope != null && !resourceTypes.contains(currentResourceType)) {
          queryStrings.add(getAclQueryString(
              Objects.isNull(currentScope.getParentScope()) ? "" : currentScope.getParentScope().toString(),
              getResourceSelector(currentScope.getLevel().getResourceType(), currentScope.getInstanceId()),
              principal.getPrincipalType().name(), principal.getPrincipalIdentifier(),
              permissionCheckDTO.getPermission()));
          currentScope = currentScope.getParentScope();
          if (currentScope != null) {
            currentResourceType = currentScope.getLevel().getResourceType();
          }
        }
      } else {
        queryStrings.add(getAclQueryString("",
            getResourceSelector(permissionCheckDTO.getResourceType(), permissionCheckDTO.getResourceIdentifier()),
            principal.getPrincipalType().name(), principal.getPrincipalIdentifier(),
            permissionCheckDTO.getPermission()));
      }

      List<ACL> aclsInDB = aclRepository.getByAclQueryStringInAndEnabled(queryStrings, true);

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
