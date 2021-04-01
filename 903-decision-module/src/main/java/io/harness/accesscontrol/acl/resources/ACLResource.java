package io.harness.accesscontrol.acl.resources;

import static io.harness.exception.WingsException.USER;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.clients.AccessCheckRequestDTO;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.dto.PrincipalType;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Objects;
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

  @POST
  @ApiOperation(value = "Check for access to resources", nickname = "getAccessControlList")
  public ResponseDTO<AccessCheckResponseDTO> get(@Valid @NotNull AccessCheckRequestDTO dto) {
    io.harness.security.dto.Principal principalInContext = SecurityContextBuilder.getPrincipal();
    Principal principalToCheckPermissions = dto.getPrincipal();

    checkForValidContextOrThrow(principalInContext);

    if (serviceContextAndNoPrincipalInBody(principalInContext, principalToCheckPermissions)) {
      return ResponseDTO.newResponse(
          AccessCheckResponseDTO.builder()
              .principal(Principal.builder()
                             .principalType(io.harness.accesscontrol.principals.PrincipalType.SERVICE)
                             .principalIdentifier(principalInContext.getName())
                             .build())
              .accessControlList(dto.getPermissions()
                                     .stream()
                                     .map(permission
                                         -> AccessControlDTO.builder()
                                                .resourceType(permission.getResourceType())
                                                .resourceIdentifier(permission.getResourceIdentifier())
                                                .permission(permission.getPermission())
                                                .resourceScope(permission.getResourceScope())
                                                .permitted(true)
                                                .build())
                                     .collect(Collectors.toList()))
              .build());
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

    // otherwise forward the call ahead and return response
    return ResponseDTO.newResponse(aclService.checkAccess(principalToCheckPermissions, dto.getPermissions()));
  }
}
