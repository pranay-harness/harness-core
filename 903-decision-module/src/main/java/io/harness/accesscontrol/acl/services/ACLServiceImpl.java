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
import io.harness.exception.InvalidRequestException;
import io.harness.security.dto.PrincipalType;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class ACLServiceImpl implements ACLService {
  private final ACLDAO aclDAO;
  private final AccessControlPreferenceService accessControlPreferenceService;

  private String validateAndGetAccountIdentifierOrThrow(List<PermissionCheckDTO> permissionCheckDTOList) {
    Set<String> accountIdentifiersWithoutResourceScope =
        permissionCheckDTOList.stream()
            .filter(
                x -> x.getResourceScope() == null || StringUtils.isEmpty(x.getResourceScope().getAccountIdentifier()))
            .map(PermissionCheckDTO::getResourceIdentifier)
            .collect(Collectors.toSet());

    Set<String> accountIdentifiersWithResourceScope =
        permissionCheckDTOList.stream()
            .map(PermissionCheckDTO::getResourceScope)
            .filter(x -> x != null && !StringUtils.isEmpty(x.getAccountIdentifier()))
            .collect(Collectors.groupingBy(ResourceScope::getAccountIdentifier))
            .keySet();

    Set<String> union = Sets.union(accountIdentifiersWithResourceScope, accountIdentifiersWithoutResourceScope);

    if (union.size() != 1) {
      throw new InvalidRequestException(
          "Checking permissions for multiple/zero account(s) in an API call is not allowed", USER);
    }
    return union.iterator().next();
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

  private boolean serviceContextAndNoPrincipalInBody(io.harness.security.dto.Principal principalInContext,
      io.harness.accesscontrol.Principal principalToCheckPermissions) {
    /*
     bypass access control check if principal in context is SERVICE and principalToCheckPermissions is either null or
     the same service principal
     */
    Optional<io.harness.security.dto.Principal> serviceCall =
        Optional.ofNullable(principalInContext).filter(x -> PrincipalType.SERVICE.equals(x.getType()));

    return serviceCall.isPresent()
        && (principalToCheckPermissions == null
            || Objects.equals(serviceCall.get().getName(), principalToCheckPermissions.getPrincipalIdentifier()));
  }

  private boolean userContextAndDifferentPrincipalInBody(io.harness.security.dto.Principal principalInContext,
      io.harness.accesscontrol.Principal principalToCheckPermissions) {
    /* apply access control checks if a principal of type other than SERVICE is trying to check permissions for any
       other principal */
    Optional<io.harness.security.dto.Principal> nonServiceCall =
        Optional.ofNullable(principalInContext).filter(x -> !PrincipalType.SERVICE.equals(x.getType()));
    return nonServiceCall.isPresent()
        && (principalToCheckPermissions != null
            && !Objects.equals(principalInContext.getName(), principalToCheckPermissions.getPrincipalIdentifier()));
  }

  private void checkForValidContextOrThrow(io.harness.security.dto.Principal principalInContext) {
    if (principalInContext == null || principalInContext.getName() == null || principalInContext.getType() == null) {
      throw new InvalidRequestException("Missing principal in context.", USER);
    }
  }

  private boolean notPresent(io.harness.accesscontrol.Principal principal) {
    return !Optional.ofNullable(principal).map(Principal::getPrincipalIdentifier).filter(x -> !x.isEmpty()).isPresent();
  }

  @Override
  public AccessCheckResponseDTO checkAccess(io.harness.security.dto.Principal principalInContext,
      Principal principalToCheckPermissions, List<PermissionCheckDTO> permissions) {
    String accountIdentifier = validateAndGetAccountIdentifierOrThrow(permissions);
    if (!accessControlPreferenceService.isAccessControlEnabled(accountIdentifier)) {
      return AccessCheckResponseDTO.builder()
          .accessControlList(permissions.stream()
                                 .map(permissionCheckDTO -> getAccessControlDTO(permissionCheckDTO, true))
                                 .collect(Collectors.toList()))
          .principal(principalToCheckPermissions)
          .build();
    }

    checkForValidContextOrThrow(principalInContext);

    if (serviceContextAndNoPrincipalInBody(principalInContext, principalToCheckPermissions)) {
      return AccessCheckResponseDTO.builder()
          .principal(Principal.builder()
                         .principalType(io.harness.accesscontrol.principals.PrincipalType.SERVICE)
                         .principalIdentifier(principalInContext.getName())
                         .build())
          .accessControlList(permissions.stream()
                                 .map(permission
                                     -> AccessControlDTO.builder()
                                            .resourceType(permission.getResourceType())
                                            .resourceIdentifier(permission.getResourceIdentifier())
                                            .permission(permission.getPermission())
                                            .resourceScope(permission.getResourceScope())
                                            .permitted(true)
                                            .build())
                                 .collect(Collectors.toList()))
          .build();
    }

    if (userContextAndDifferentPrincipalInBody(principalInContext, principalToCheckPermissions)) {
      // a user principal needs elevated permissions to check for permissions of another principal
      // TODO{phoenikx} Apply access check here
      log.debug("checking for access control checks here...");
    }

    if (notPresent(principalToCheckPermissions)) {
      principalToCheckPermissions =
          Principal.builder()
              .principalIdentifier(principalInContext.getName())
              .principalType(io.harness.accesscontrol.principals.PrincipalType.fromSecurityPrincipalType(
                  principalInContext.getType()))
              .build();
    }

    List<ACL> accessControlList = aclDAO.get(principalToCheckPermissions, permissions);
    List<AccessControlDTO> accessControlDTOList = new ArrayList<>();
    for (int i = 0; i < permissions.size(); i++) {
      PermissionCheckDTO permissionCheckDTO = permissions.get(i);
      accessControlDTOList.add(getAccessControlDTO(permissionCheckDTO, accessControlList.get(i) != null));
    }
    return AccessCheckResponseDTO.builder()
        .principal(principalToCheckPermissions)
        .accessControlList(accessControlDTOList)
        .build();
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
