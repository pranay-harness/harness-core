package software.wings.resources;

import static io.harness.annotations.dev.HarnessModule._970_RBAC_CORE;

import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.rest.RestResponse;

import software.wings.beans.security.AccessRequest;
import software.wings.beans.security.AccessRequestDTO;
import software.wings.security.annotations.ApiKeyAuthorized;
import software.wings.service.intfc.AccessRequestService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;

@Api("accessRequest")
@Path("/accessRequest")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
@OwnedBy(HarnessTeam.PL)
@TargetModule(_970_RBAC_CORE)
public class AccessRequestResource {
  private final AccessRequestService accessRequestService;

  @Inject
  public AccessRequestResource(AccessRequestService accessRequestService) {
    this.accessRequestService = accessRequestService;
  }

  @POST
  @Path("{accountId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiKeyAuthorized(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<AccessRequest> create(
      @PathParam("accountId") String accountId, @RequestBody @NotNull AccessRequestDTO accessRequestDTO) {
    return new RestResponse<>(accessRequestService.createAccessRequest(accessRequestDTO));
  }

  @DELETE
  @Path("{accountId}/{accessRequestId}")
  @ApiKeyAuthorized(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Boolean> delete(
      @PathParam("accountId") String accountId, @PathParam("accessRequestId") String accessRequestId) {
    return new RestResponse<>(accessRequestService.delete(accessRequestId));
  }

  @GET
  @Path("{accountId}/listAccessRequest")
  @ApiKeyAuthorized(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<AccessRequest> listAccessRequest(
      @PathParam("accountId") String accountId, @QueryParam("accessRequestId") String accessRequestId) {
    return new RestResponse<>(accessRequestService.get(accessRequestId));
  }

  @GET
  @Path("{accountId}/listAccessRequest/harnessUserGroup")
  @ApiKeyAuthorized(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<List<AccessRequest>> listActiveAccessRequest(
      @PathParam("accountId") String accountId, @QueryParam("harnessUserGroupId") String harnessUserGroupId) {
    return new RestResponse<>(accessRequestService.getActiveAccessRequest(harnessUserGroupId));
  }

  @GET
  @Path("{accountId}/listAccessRequest/account")
  @ApiKeyAuthorized(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<List<AccessRequest>> listActiveAccessRequestForAccount(@PathParam("accountId") String accountId) {
    return new RestResponse<>(accessRequestService.getActiveAccessRequestForAccount(accountId));
  }
}
