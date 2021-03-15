package io.harness.accesscontrol.acl.resources;

import static io.harness.exception.WingsException.USER;

import io.harness.accesscontrol.HPrincipal;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.clients.HAccessCheckRequestDTO;
import io.harness.accesscontrol.clients.HAccessCheckResponseDTO;
import io.harness.accesscontrol.clients.HAccessControlDTO;
import io.harness.exception.AccessDeniedException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.dto.Principal;
import io.harness.security.dto.PrincipalType;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
public class HACLResource {
  private final ACLService aclService;

  private boolean shouldBypassAccessControlChecks(
      Principal principalInContext, io.harness.accesscontrol.Principal principalToCheckPermissions) {
    /*
     bypass access control check if principal in context is SERVICE and principalToCheckPermissions is either null or
     the same service principal
     */
    Optional<Principal> serviceCall =
        Optional.ofNullable(principalInContext).filter(x -> PrincipalType.SERVICE.equals(x.getType()));

    return serviceCall.isPresent()
        && (principalToCheckPermissions == null
            || serviceCall.get().getName().equals(principalToCheckPermissions.getPrincipalIdentifier()));
  }

  private boolean shouldApplyAccessControlCheckToQueryPermissions(
      Principal principalInContext, io.harness.accesscontrol.Principal principalToCheckPermissions) {
    /* apply access control checks if a principal of type other than SERVICE is trying to check permissions for any
       other principal */
    Optional<Principal> nonServiceCall =
        Optional.ofNullable(principalInContext).filter(x -> !PrincipalType.SERVICE.equals(x.getType()));
    return nonServiceCall.isPresent()
        && (principalToCheckPermissions != null
            && !principalInContext.getName().equals(principalToCheckPermissions.getPrincipalIdentifier()));
  }

  @POST
  @ApiOperation(value = "Check for access to resources", nickname = "getAccessControlList")
  public ResponseDTO<HAccessCheckResponseDTO> get(@Valid @NotNull HAccessCheckRequestDTO dto) {
    Principal principalInContext = SecurityContextBuilder.getPrincipal();
    io.harness.accesscontrol.HPrincipal principalToCheckPermissions = dto.getPrincipal();

    // check if context is valid
    if (principalInContext == null || principalInContext.getName() == null || principalInContext.getType() == null) {
      throw new AccessDeniedException("Missing principal in context.", USER);
    }

    // if access check is to be bypassed, directly return response
    if (shouldBypassAccessControlChecks(principalInContext, principalToCheckPermissions)) {
      return ResponseDTO.newResponse(
          HAccessCheckResponseDTO.builder()
              .principal(HPrincipal.builder()
                             .principalType(io.harness.accesscontrol.principals.PrincipalType.SERVICE)
                             .principalIdentifier(principalInContext.getName())
                             .build())
              .accessControlList(dto.getPermissions()
                                     .stream()
                                     .map(permission
                                         -> HAccessControlDTO.builder()
                                                .resourceType(permission.getResourceType())
                                                .resourceIdentifier(permission.getResourceIdentifier())
                                                .permission(permission.getPermission())
                                                .resourceScope(permission.getResourceScope())
                                                .accessible(true)
                                                .build())
                                     .collect(Collectors.toList()))
              .build());
    }

    if (shouldApplyAccessControlCheckToQueryPermissions(principalInContext, principalToCheckPermissions)) {
      // apply RBAC checks here
      log.debug("checking for access control checks here...");
    }

    // otherwise forward the call ahead and return response
    return ResponseDTO.newResponse(
        (HAccessCheckResponseDTO) aclService.checkAccess(principalToCheckPermissions, dto.getPermissions()));
  }
}
