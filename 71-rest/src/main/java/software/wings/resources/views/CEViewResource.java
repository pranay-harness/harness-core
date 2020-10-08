package software.wings.resources.views;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.service.CEReportScheduleService;
import io.harness.ccm.views.service.CEViewService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api("view")
@Path("/view")
@Produces("application/json")
public class CEViewResource {
  private CEViewService ceViewService;
  private CEReportScheduleService ceReportScheduleService;

  @Inject
  public CEViewResource(CEViewService ceViewService, CEReportScheduleService ceReportScheduleService) {
    this.ceViewService = ceViewService;
    this.ceReportScheduleService = ceReportScheduleService;
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<CEView> saveCustomField(
      @QueryParam("accountId") String accountId, @Valid @RequestBody CEView ceView) {
    ceView.setAccountId(accountId);
    return new RestResponse<>(ceViewService.save(ceView));
  }

  @PUT
  @Timed
  @ExceptionMetered
  public RestResponse<CEView> modifyCustomField(
      @QueryParam("accountId") String accountId, @Valid @RequestBody CEView ceView) {
    ceView.setAccountId(accountId);
    return new RestResponse<>(ceViewService.update(ceView));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  public Response delete(@QueryParam("accountId") String accountId, @QueryParam("viewId") String viewId) {
    ceViewService.delete(viewId, accountId);
    ceReportScheduleService.deleteAllByView(viewId, accountId);
    RestResponse rr = new RestResponse("Successfully deleted the view");
    return prepareResponse(rr, Response.Status.OK);
  }

  private Response prepareResponse(RestResponse restResponse, Response.Status status) {
    return Response.status(status).entity(restResponse).type(MediaType.APPLICATION_JSON).build();
  }
}
