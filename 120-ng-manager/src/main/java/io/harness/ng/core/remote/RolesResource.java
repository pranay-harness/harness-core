package io.harness.ng.core.remote;

import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.RoleDTO;
import io.harness.ng.core.models.Role;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/roles")
@Path("/organizations/{orgIdentifier}/projects/{projectIdentifier}/roles")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class RolesResource {
  @GET
  @ApiOperation(value = "Get all roles for the queried project", nickname = "getRoles")
  public ResponseDTO<Optional<List<RoleDTO>>> get(@QueryParam("accountIdentifier") @NotNull String accountIdentifier,
      @PathParam("orgIdentifier") @NotNull String orgIdentifier,
      @PathParam("projectIdentifier") @NotNull String projectIdentifier) {
    List<Role> rolesList = getRolesList(accountIdentifier, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(Optional.of(rolesList.stream().map(RoleMapper::writeDTO).collect(toList())));
  }

  List<Role> getRolesList(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<Role> rolesList = new ArrayList<>();
    List<String> roleNames = Arrays.asList("Project Admin", "Project Member", "Project Viewer");
    for (String roleName : roleNames) {
      Role role = Role.builder()
                      .accountIdentifier(accountIdentifier)
                      .orgIdentifier(orgIdentifier)
                      .projectIdentifier(projectIdentifier)
                      .name(roleName)
                      .build();
      rolesList.add(role);
    }
    return rolesList;
  }
}
