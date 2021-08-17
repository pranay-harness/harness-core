package io.harness.accesscontrol.acl.persistence;

import static io.harness.accesscontrol.acl.persistence.ACL.getAclQueryString;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.PermissionCheck;
import io.harness.accesscontrol.acl.persistence.repositories.ACLRepository;
import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private final Set<String> scopeResourceTypes;

  @Inject
  public ACLDAOImpl(@Named(ACL.PRIMARY_COLLECTION) ACLRepository aclRepository, Map<String, ScopeLevel> scopeLevels) {
    this.aclRepository = aclRepository;
    this.scopeResourceTypes =
        scopeLevels.values().stream().map(ScopeLevel::getResourceType).collect(Collectors.toSet());
  }

  private String getResourceSelector(String resourceType, String resourceIdentifier) {
    return PATH_DELIMITER.concat(resourceType).concat(PATH_DELIMITER).concat(resourceIdentifier);
  }

  private Set<String> getQueryStrings(PermissionCheck permissionCheck, Principal principal) {
    String scope =
        Optional.ofNullable(permissionCheck.getResourceScope()).flatMap(rs -> Optional.of(rs.toString())).orElse("");
    Set<String> queryStrings = new HashSet<>();
    String resourceType = permissionCheck.getResourceType();
    String resourceIdentifier = permissionCheck.getResourceIdentifier();

    // query for resource=/RESOURCE_TYPE/{resourceIdentifier} in given scope
    if (!StringUtils.isEmpty(resourceIdentifier)) {
      queryStrings.add(getAclQueryString(scope, getResourceSelector(resourceType, resourceIdentifier),
          principal.getPrincipalType().name(), principal.getPrincipalIdentifier(), permissionCheck.getPermission()));
    }

    // query for resource=/RESOURCE_TYPE/* in given scope
    queryStrings.add(getAclQueryString(scope, getResourceSelector(resourceType, "*"),
        principal.getPrincipalType().name(), principal.getPrincipalIdentifier(), permissionCheck.getPermission()));

    // query for resource=/*/* in given scope
    queryStrings.add(getAclQueryString(scope, getResourceSelector("*", "*"), principal.getPrincipalType().name(),
        principal.getPrincipalIdentifier(), permissionCheck.getPermission()));

    // if RESOURCE_TYPE is a scope, query for scope = {given_scope}/RESOURCE_TYPE/{resourceIdentifier}
    if (scopeResourceTypes.contains(resourceType)) {
      scope = scope.concat(PATH_DELIMITER + resourceType + PATH_DELIMITER + resourceIdentifier);

      // and resource = /RESOURCE_TYPE/{resourceIdentifier}
      if (!StringUtils.isEmpty(resourceIdentifier)) {
        queryStrings.add(getAclQueryString(scope, getResourceSelector(resourceType, resourceIdentifier),
            principal.getPrincipalType().name(), principal.getPrincipalIdentifier(), permissionCheck.getPermission()));
      }

      // and resource = /RESOURCE_TYPE/*
      queryStrings.add(getAclQueryString(scope, getResourceSelector(resourceType, "*"),
          principal.getPrincipalType().name(), principal.getPrincipalIdentifier(), permissionCheck.getPermission()));

      // and resource = /*/*
      queryStrings.add(getAclQueryString(scope, getResourceSelector("*", "*"), principal.getPrincipalType().name(),
          principal.getPrincipalIdentifier(), permissionCheck.getPermission()));
    }
    return queryStrings;
  }

  @Override
  public List<Boolean> checkForAccess(Principal principal, List<PermissionCheck> permissionChecks) {
    List<Set<String>> aclQueryStringsPerPermission = new ArrayList<>();
    List<String> aclQueryStrings = new ArrayList<>();
    permissionChecks.forEach(permissionCheck -> {
      Set<String> queryStrings = getQueryStrings(permissionCheck, principal);
      aclQueryStringsPerPermission.add(queryStrings);
      aclQueryStrings.addAll(queryStrings);
    });

    Set<String> aclsPresentInDB = aclRepository.getByAclQueryStringInAndEnabled(aclQueryStrings, true);
    return aclQueryStringsPerPermission.stream()
        .map(queryStringsForPermission -> queryStringsForPermission.stream().anyMatch(aclsPresentInDB::contains))
        .collect(Collectors.toList());
  }
}
