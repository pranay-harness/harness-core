package io.harness.ng.core.invites.remote;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.invites.dto.RoleDTO;
import io.harness.ng.core.invites.entities.Role;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Api("/roles")
@Path("/roles")
@Produces({"application/json", "text/yaml"})
@Consumes({"application/json", "text/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@OwnedBy(HarnessTeam.PL)
public class RolesResource {
  @GET
  @ApiOperation(value = "Get all roles for the queried project", nickname = "getRoles")
  public ResponseDTO<Optional<List<RoleDTO>>> get(@QueryParam("accountIdentifier") @NotNull String accountIdentifier,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier) {
    List<Role> rolesList = getRolesList(accountIdentifier, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(Optional.of(rolesList.stream().map(RoleMapper::writeDTO).collect(toList())));
  }

  List<Role> getRolesList(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<Role> rolesList = new ArrayList<>();
    List<String> projectRolesList = Arrays.asList("Project Admin", "Project Member", "Project Viewer");
    List<String> orgRolesList = Arrays.asList("Organization Admin", "Organization Member", "Organization Viewer");
    if (isEmpty(projectIdentifier)) {
      for (String roleName : orgRolesList) {
        Role role =
            Role.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).name(roleName).build();
        rolesList.add(role);
      }
    } else {
      for (String roleName : projectRolesList) {
        Role role = Role.builder()
                        .accountIdentifier(accountIdentifier)
                        .orgIdentifier(orgIdentifier)
                        .projectIdentifier(projectIdentifier)
                        .name(roleName)
                        .build();
        rolesList.add(role);
      }
    }
    return rolesList;
  }
}
