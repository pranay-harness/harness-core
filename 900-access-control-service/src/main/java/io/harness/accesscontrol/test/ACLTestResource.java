package io.harness.accesscontrol.test;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/acl")
@Api("/acl")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class ACLTestResource {
  private final AccessControlClient accessControlClient;

  @Inject
  public ACLTestResource(@Named("NON_PRIVILEGED") AccessControlClient accessControlClient) {
    this.accessControlClient = accessControlClient;
  }

  @GET
  @Path("/acl-test")
  @ApiOperation(value = "Test ACL", nickname = "testACL")
  public ResponseDTO<String> get(@QueryParam("account") @AccountIdentifier String account,
      @QueryParam("org") @OrgIdentifier String org, @QueryParam("project") @ProjectIdentifier String project,
      @QueryParam("resourceIdentifier") @ResourceIdentifier String resourceIdentifier) {
    accessControlClient.checkForAccessOrThrow(PermissionCheckDTO.builder()
                                                  .resourceType("SECRET")
                                                  .resourceIdentifier(resourceIdentifier)
                                                  .resourceScope(ResourceScope.builder()
                                                                     .accountIdentifier(account)
                                                                     .orgIdentifier(org)
                                                                     .projectIdentifier(project)
                                                                     .build())
                                                  .permission("core.secret.create")
                                                  .build());

    return ResponseDTO.newResponse("accessible");
  }
}
