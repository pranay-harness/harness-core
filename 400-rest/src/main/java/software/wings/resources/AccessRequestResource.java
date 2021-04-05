package software.wings.resources;

import io.harness.rest.RestResponse;

import software.wings.beans.security.AccessRequest;
import software.wings.service.intfc.AccessRequestService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestParam;

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

//  @GET
//  @Path("create")
//  public RestResponse<AccessRequest> create(
//      String accountId, String harnessUserGroupId, long accessStartAt, long accessEndAt) {
//    return new RestResponse<>(
//        accessRequestService.create(accountId, harnessUserGroupId, accessStartAt, accessEndAt, true));
//  }

    @GET
    @Path("/delete")
    public RestResponse<Boolean> delete(@QueryParam("accountId") String accountId, @QueryParam("accessRequestId") String accessRequestId){
      return new RestResponse<>(accessRequestService.delete(accessRequestId));
    }
  //
  //  @GET
  //  @Path("update")
  //  public RestResponse<AccessRequest> update(String accountId, String accessRequestId, long accessStartAt, long
  //  accessEndAt){
  //    return new RestResponse<>(accessRequestService.update(accessRequestId, accessStartAt, accessEndAt));
  //  }
  //
  //  @GET
  //  @Path("updateStatus")
  //  public RestResponse<AccessRequest> updateStatus(String accountId, String accessRequestId, boolean accessStatus){
  //    return new RestResponse<>(accessRequestService.updateStatus(accessRequestId, accessStatus));
  //  }
  //
}
