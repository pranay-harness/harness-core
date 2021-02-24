package io.harness.accesscontrol.acl.resources;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;

@Path("/acl")
@Api("/acl")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@NextGenManagerAuth
public class ACLTestResource {
  @GET
  @Path("/acl-test")
  @ApiOperation(value = "Test ACL", nickname = "testACL")
  @NGAccessControlCheck(resourceType = "SECRET_MANAGER", permissionIdentifier = "core.secretManager.create")
  public ResponseDTO<String> get(@QueryParam("account") @AccountIdentifier String account,
      @QueryParam("org") @OrgIdentifier String org, @QueryParam("project") @ProjectIdentifier String project,
      @QueryParam("resourceIdentifier") @ResourceIdentifier String resourceIdentifier) {
    return ResponseDTO.newResponse("accessible");
  }
}
