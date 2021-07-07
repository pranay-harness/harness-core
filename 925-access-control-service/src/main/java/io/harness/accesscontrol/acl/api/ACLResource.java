package io.harness.accesscontrol.acl.api;

import static io.harness.accesscontrol.acl.api.ACLResourceHelper.checkForValidContextOrThrow;
import static io.harness.accesscontrol.acl.api.ACLResourceHelper.getAccessControlDTO;
import static io.harness.accesscontrol.acl.api.ACLResourceHelper.getAccountIdentifier;
import static io.harness.accesscontrol.acl.api.ACLResourceHelper.notPresent;
import static io.harness.accesscontrol.acl.api.ACLResourceHelper.serviceContextAndNoPrincipalInBody;
import static io.harness.accesscontrol.acl.api.ACLResourceHelper.userContextAndDifferentPrincipalInBody;
import static io.harness.accesscontrol.principals.PrincipalType.fromSecurityPrincipalType;
import static io.harness.exception.WingsException.USER;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.ACLService;
import io.harness.accesscontrol.clients.AccessCheckRequestDTO;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.accesscontrol.preference.services.AccessControlPreferenceService;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
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

  @POST
  @ApiOperation(value = "Check for access to resources", nickname = "getAccessControlList")
  public ResponseDTO<io.harness.accesscontrol.clients.AccessCheckResponseDTO> get(
      @Valid @NotNull AccessCheckRequestDTO dto) {
    io.harness.security.dto.Principal contextPrincipal = SecurityContextBuilder.getPrincipal();
    List<PermissionCheckDTO> permissions = dto.getPermissions();
    Principal principalToCheckPermissions = dto.getPrincipal();
    Optional<String> accountIdentifierOptional = getAccountIdentifier(permissions);

    if (accountIdentifierOptional.isPresent()
        && !accessControlPreferenceService.isAccessControlEnabled(accountIdentifierOptional.get())) {
      return ResponseDTO.newResponse(
          io.harness.accesscontrol.clients.AccessCheckResponseDTO.builder()
              .accessControlList(permissions.stream()
                                     .map(permissionCheckDTO -> getAccessControlDTO(permissionCheckDTO, true))
                                     .collect(Collectors.toList()))
              .principal(principalToCheckPermissions)
              .build());
    }

    checkForValidContextOrThrow(contextPrincipal);

    if (serviceContextAndNoPrincipalInBody(contextPrincipal, principalToCheckPermissions)) {
      return ResponseDTO.newResponse(
          io.harness.accesscontrol.clients.AccessCheckResponseDTO.builder()
              .principal(Principal.builder()
                             .principalType(PrincipalType.SERVICE)
                             .principalIdentifier(contextPrincipal.getName())
                             .build())
              .accessControlList(permissions.stream()
                                     .map(permission -> getAccessControlDTO(permission, true))
                                     .collect(Collectors.toList()))
              .build());
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

    AccessCheckResponseDTO accessCheckResponseDTO =
        aclService.checkAccess(principalToCheckPermissions, dto.getPermissions());
    return ResponseDTO.newResponse(accessCheckResponseDTO);
  }
}
