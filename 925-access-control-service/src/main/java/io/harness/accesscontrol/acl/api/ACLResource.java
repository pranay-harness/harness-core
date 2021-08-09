package io.harness.accesscontrol.acl.api;

import static io.harness.accesscontrol.clients.AccessControlClientUtils.checkPreconditions;
import static io.harness.accesscontrol.clients.AccessControlClientUtils.getAccessControlDTO;
import static io.harness.accesscontrol.clients.AccessControlClientUtils.serviceContextAndNoPrincipalInBody;
import static io.harness.accesscontrol.principals.PrincipalType.SERVICE;
import static io.harness.accesscontrol.principals.PrincipalType.fromSecurityPrincipalType;
import static io.harness.exception.WingsException.USER;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.ACLService;
import io.harness.accesscontrol.acl.PermissionCheckResult;
import io.harness.accesscontrol.clients.AccessCheckRequestDTO;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.preference.services.AccessControlPreferenceService;
import io.harness.accesscontrol.roleassignments.privileged.PrivilegedAccessCheck;
import io.harness.accesscontrol.roleassignments.privileged.PrivilegedAccessResult;
import io.harness.accesscontrol.roleassignments.privileged.PrivilegedRoleAssignmentService;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Path("/acl")
@Api("/acl")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
@OwnedBy(HarnessTeam.PL)
public class ACLResource {
  private final ACLService aclService;
  private final AccessControlPreferenceService accessControlPreferenceService;
  private final PrivilegedRoleAssignmentService privilegedRoleAssignmentService;

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

  private boolean notPresent(Principal principal) {
    return !Optional.ofNullable(principal).map(Principal::getPrincipalIdentifier).filter(x -> !x.isEmpty()).isPresent();
  }

  @POST
  @ApiOperation(value = "Check for access to resources", nickname = "getAccessControlList")
  public ResponseDTO<AccessCheckResponseDTO> get(@Valid @NotNull AccessCheckRequestDTO dto) {
    io.harness.security.dto.Principal contextPrincipal = SecurityContextBuilder.getPrincipal();
    List<PermissionCheckDTO> permissionChecks = dto.getPermissions();
    Principal principalToCheckPermissionsFor = dto.getPrincipal();

    boolean preconditionsValid = checkPreconditions(contextPrincipal, principalToCheckPermissionsFor);

    if (serviceContextAndNoPrincipalInBody(contextPrincipal, principalToCheckPermissionsFor)) {
      return ResponseDTO.newResponse(
          AccessCheckResponseDTO.builder()
              .principal(Principal.of(SERVICE, contextPrincipal.getName()))
              .accessControlList(permissionChecks.stream()
                                     .map(permissionCheckDTO
                                         -> AccessControlDTO.builder()
                                                .permitted(true)
                                                .permission(permissionCheckDTO.getPermission())
                                                .resourceScope(permissionCheckDTO.getResourceScope())
                                                .resourceIdentifier(permissionCheckDTO.getResourceIdentifier())
                                                .resourceType(permissionCheckDTO.getResourceType())
                                                .build())
                                     .collect(Collectors.toList()))
              .build());
    }

    if (!preconditionsValid) {
      throw new InvalidRequestException(
          "Missing principal in context or User doesn't have permission to check access for a different principal",
          USER);
    }

    Optional<String> accountIdentifierOptional = getAccountIdentifier(permissionChecks);
    if (accountIdentifierOptional.isPresent()
        && !accessControlPreferenceService.isAccessControlEnabled(accountIdentifierOptional.get())) {
      return ResponseDTO.newResponse(
          AccessCheckResponseDTO.builder()
              .accessControlList(permissionChecks.stream()
                                     .map(permissionCheckDTO -> getAccessControlDTO(permissionCheckDTO, true))
                                     .collect(Collectors.toList()))
              .principal(principalToCheckPermissionsFor)
              .build());
    }

    if (notPresent(principalToCheckPermissionsFor)) {
      principalToCheckPermissionsFor =
          Principal.of(fromSecurityPrincipalType(contextPrincipal.getType()), contextPrincipal.getName());
    }

    List<PermissionCheckResult> permissionCheckResults = aclService.checkAccess(principalToCheckPermissionsFor,
        permissionChecks.stream().map(PermissionCheckDTOMapper::fromDTO).collect(Collectors.toList()));

    AccessCheckResponseDTO accessCheckResponseDTO =
        AccessCheckResponseDTO.builder()
            .principal(principalToCheckPermissionsFor)
            .accessControlList(
                permissionCheckResults.stream().map(PermissionCheckDTOMapper::toDTO).collect(Collectors.toList()))
            .build();

    if (accountIdentifierOptional.isPresent()) {
      io.harness.accesscontrol.principals.Principal principal =
          io.harness.accesscontrol.principals.Principal.builder()
              .principalIdentifier(principalToCheckPermissionsFor.getPrincipalIdentifier())
              .principalType(principalToCheckPermissionsFor.getPrincipalType())
              .build();
      PrivilegedAccessCheck privilegedAccessCheck = PrivilegedAccessCheck.builder()
                                                        .principal(principal)
                                                        .accountIdentifier(accountIdentifierOptional.get())
                                                        .permissions(permissionChecks)
                                                        .build();
      PrivilegedAccessResult privilegedAccessResult =
          privilegedRoleAssignmentService.checkAccess(privilegedAccessCheck);
      Iterator<AccessControlDTO> iterator = accessCheckResponseDTO.getAccessControlList().iterator();
      int index = 0;
      while (iterator.hasNext()) {
        AccessControlDTO rbacAccess = iterator.next();
        AccessControlDTO privilegedAccess = privilegedAccessResult.getPermissionCheckResults().get(index);
        rbacAccess.setPermitted(privilegedAccess.isPermitted() || rbacAccess.isPermitted());
        index++;
      }
    }
    return ResponseDTO.newResponse(accessCheckResponseDTO);
  }
}
