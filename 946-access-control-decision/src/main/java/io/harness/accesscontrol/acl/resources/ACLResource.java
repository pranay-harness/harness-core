package io.harness.accesscontrol.acl.resources;

import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.clients.AccessCheckRequestDTO;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.dto.Principal;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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

  @POST
  @ApiOperation(value = "Check for access to resources", nickname = "getAccessControlList")
  public ResponseDTO<AccessCheckResponseDTO> get(@Valid @NotNull AccessCheckRequestDTO dto) {
    Principal contextPrincipal = SecurityContextBuilder.getPrincipal();
    // TODO: make the level to DEBUG after things get stable
    log.info("Access check request with contextPrincipal: {}, requestPrincipal: {} and permissions: {}",
        contextPrincipal, dto.getPrincipal(),
        dto.getPermissions().stream().map(PermissionCheckDTO::getPermission).collect(Collectors.toList()));

    AccessCheckResponseDTO accessCheckResponseDTO = aclService.checkAccess(SecurityContextBuilder.getPrincipal(), dto);
    log.info("Permitted list: {}",
        accessCheckResponseDTO.getAccessControlList()
            .stream()
            .map(AccessControlDTO::isPermitted)
            .collect(Collectors.toList()));
    return ResponseDTO.newResponse(accessCheckResponseDTO);
  }
}
