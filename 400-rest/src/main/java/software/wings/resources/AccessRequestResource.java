package software.wings.resources;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import software.wings.beans.security.AccessRequest;
import software.wings.service.intfc.AccessRequestService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@Api("accessReqeust")
@Path("/accessRequest")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class AccessRequestResource {
  private final AccessRequestService accessRequestService;

  @Inject
  public AccessRequestResource(AccessRequestService accessRequestService) {
    this.accessRequestService = accessRequestService;
  }

  @GET
  @Path("create")
  public RestResponse<AccessRequest> create(@QueryParam("accountId") String accountId,
      @QueryParam("harnessUserGroupId") String harnessUserGroupId, @QueryParam("accessStartAt") long accessStartAt,
      @QueryParam("accessEndAt") long accessEndAt) {
    return new RestResponse<>(
        accessRequestService.create(accountId, harnessUserGroupId, accessStartAt, accessEndAt, true));
  }

  @GET
  @Path("/delete")
  public RestResponse<Boolean> delete(
      @QueryParam("accountId") String accountId, @QueryParam("accessRequestId") String accessRequestId) {
    return new RestResponse<>(accessRequestService.delete(accessRequestId));
  }

  @GET
  @Path("update")
  public RestResponse<AccessRequest> update(@QueryParam("accountId") String accountId,
      @QueryParam("accessRequestId") String accessRequestId, @QueryParam("accessStartAt") long accessStartAt,
      @QueryParam("accessEndAt") long accessEndAt) {
    return new RestResponse<>(accessRequestService.update(accessRequestId, accessStartAt, accessEndAt));
  }

  @GET
  @Path("update/status")
  public RestResponse<AccessRequest> updateStatus(@QueryParam("accountId") String accountId,
      @QueryParam("accessRequestId") String accessRequestId,
      @QueryParam("accessStatus") @DefaultValue("false") boolean accessStatus) {
    return new RestResponse<>(accessRequestService.updateStatus(accessRequestId, accessStatus));
  }

  @GET
  @Path("listAccessRequest")
  public RestResponse<AccessRequest> listAccessRequest(
      @QueryParam("accountId") String accountId, @QueryParam("accessRequestId") String accessRequestId) {
    return new RestResponse<>(accessRequestService.get(accessRequestId));
  }

  @GET
  @Path("listAccessRequest/harnessUserGroup")
  public RestResponse<List<AccessRequest>> listActiveAccessRequest(
      @QueryParam("harnessUserGroupId") String harnessUserGroupId) {
    return new RestResponse<>(accessRequestService.getActiveAccessRequest(harnessUserGroupId));
  }

  @GET
  @Path("listAccessRequest/account")
  public RestResponse<List<AccessRequest>> listActiveAccessRequestForAccount(
      @QueryParam("accountId") String accountId) {
    return new RestResponse<>(accessRequestService.getActiveAccessRequestForAccount(accountId));
  }
}
