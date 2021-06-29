package io.harness.accesscontrol.acl.services;

import static io.harness.accesscontrol.permissions.PermissionStatus.EXPERIMENTAL;
import static io.harness.accesscontrol.permissions.PermissionStatus.INACTIVE;
import static io.harness.accesscontrol.permissions.PermissionStatus.STAGING;
import static io.harness.accesscontrol.principals.PrincipalType.fromSecurityPrincipalType;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.daos.ACLDAO;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.clients.AccessCheckRequestDTO;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.permissions.Permission;
import io.harness.accesscontrol.permissions.PermissionFilter;
import io.harness.accesscontrol.permissions.PermissionService;
import io.harness.accesscontrol.preference.services.AccessControlPreferenceService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(PL)
@Singleton
@Slf4j
public class ACLServiceImpl implements ACLService {
  private final ACLDAO aclDAO;
  private final AccessControlPreferenceService accessControlPreferenceService;
  private final Set<String> disabledPermissions;

  @Inject
  public ACLServiceImpl(ACLDAO aclDAO, AccessControlPreferenceService accessControlPreferenceService,
      PermissionService permissionService) {
    this.aclDAO = aclDAO;
    this.accessControlPreferenceService = accessControlPreferenceService;
    PermissionFilter permissionFilter =
        PermissionFilter.builder().statusFilter(Sets.newHashSet(INACTIVE, EXPERIMENTAL, STAGING)).build();
    disabledPermissions =
        permissionService.list(permissionFilter).stream().map(Permission::getIdentifier).collect(Collectors.toSet());
  }

  private Optional<String> getAccountIdentifier(List<PermissionCheckDTO> permissionCheckDTOList) {
    if (permissionCheckDTOList.isEmpty()) {
      return Optional.empty();
    }

    PermissionCheckDTO permissionCheckDTO = permissionCheckDTOList.get(0);
    if (permissionCheckDTO.getResourceScope() == null
        || StringUtils.isEmpty(permissionCheckDTO.getResourceScope().getAccountIdentifier())) {
      return Optional.of(permissionCheckDTO.getResourceIdentifier());
    } else {
      return Optional.of(permissionCheckDTO.getResourceScope().getAccountIdentifier());
    }
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
     check if principal in context is SERVICE and principalToCheckPermissions is either null or
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
    /* check if a principal of type other than SERVICE is trying to check permissions for any
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

  private AccessCheckResponseDTO checkForAccessInternal(Principal principal, List<PermissionCheckDTO> permissions) {
    List<Boolean> allowedAccessList = aclDAO.checkForAccess(principal, permissions);
    List<AccessControlDTO> accessControlDTOList = new ArrayList<>();

    for (int i = 0; i < permissions.size(); i++) {
      PermissionCheckDTO permissionCheckDTO = permissions.get(i);
      if (disabledPermissions.contains(permissionCheckDTO.getPermission())) {
        accessControlDTOList.add(getAccessControlDTO(permissionCheckDTO, true));
      } else {
        accessControlDTOList.add(getAccessControlDTO(permissionCheckDTO, allowedAccessList.get(i)));
      }
    }

    return AccessCheckResponseDTO.builder().principal(principal).accessControlList(accessControlDTOList).build();
  }

  @Override
  public AccessCheckResponseDTO checkAccess(
      io.harness.security.dto.Principal contextPrincipal, AccessCheckRequestDTO accessCheckRequestDTO) {
    List<PermissionCheckDTO> permissions = accessCheckRequestDTO.getPermissions();
    Principal principalToCheckPermissions = accessCheckRequestDTO.getPrincipal();
    Optional<String> accountIdentifierOptional = getAccountIdentifier(permissions);

    if (accountIdentifierOptional.isPresent()
        && !accessControlPreferenceService.isAccessControlEnabled(accountIdentifierOptional.get())) {
      return AccessCheckResponseDTO.builder()
          .accessControlList(permissions.stream()
                                 .map(permissionCheckDTO -> getAccessControlDTO(permissionCheckDTO, true))
                                 .collect(Collectors.toList()))
          .principal(principalToCheckPermissions)
          .build();
    }

    checkForValidContextOrThrow(contextPrincipal);

    if (serviceContextAndNoPrincipalInBody(contextPrincipal, principalToCheckPermissions)) {
      return AccessCheckResponseDTO.builder()
          .principal(Principal.builder()
                         .principalType(io.harness.accesscontrol.principals.PrincipalType.SERVICE)
                         .principalIdentifier(contextPrincipal.getName())
                         .build())
          .accessControlList(permissions.stream()
                                 .map(permission -> getAccessControlDTO(permission, true))
                                 .collect(Collectors.toList()))
          .build();
    }

    if (userContextAndDifferentPrincipalInBody(contextPrincipal, principalToCheckPermissions)) {
      // a user principal needs elevated permissions to check for permissions of another principal
      // for now, throwing exception since this is not a valid use case right now
      throw new AccessDeniedException(
          "Principal not allowed to check permission of a different principal", ErrorCode.NG_ACCESS_DENIED, USER);
    }

    if (notPresent(principalToCheckPermissions)) {
      principalToCheckPermissions =
          Principal.of(fromSecurityPrincipalType(contextPrincipal.getType()), contextPrincipal.getName());
    }
    return checkForAccessInternal(principalToCheckPermissions, permissions);
  }

  @Override
  public long saveAll(List<ACL> acls) {
    return aclDAO.saveAll(acls);
  }

  @Override
  public long deleteByRoleAssignment(String roleAssignmentId) {
    return aclDAO.deleteByRoleAssignment(roleAssignmentId);
  }

  @Override
  public List<ACL> getByUserGroup(String scope, String userGroupIdentifier) {
    return aclDAO.getByUserGroup(scope, userGroupIdentifier);
  }

  @Override
  public List<ACL> getByRole(String scope, String roleIdentifier, boolean managed) {
    return aclDAO.getByRole(scope, roleIdentifier, managed);
  }

  @Override
  public List<ACL> getByResourceGroup(String scope, String resourceGroupIdentifier, boolean managed) {
    return aclDAO.getByResourceGroup(scope, resourceGroupIdentifier, managed);
  }

  @Override
  public List<ACL> getByRoleAssignment(String roleAssignmentId) {
    return aclDAO.getByRoleAssignment(roleAssignmentId);
  }
}
