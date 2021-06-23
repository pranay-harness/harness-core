package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.commons.utils.BigQueryHelper.UNIFIED_TABLE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.commons.utils.BigQueryHelper;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.service.CEReportScheduleService;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.cloud.bigquery.BigQuery;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Api("perspective")
@Path("/perspective")
@Produces("application/json")
@NextGenManagerAuth
@Slf4j
@Service
@OwnedBy(CE)
public class PerspectiveResource {
  private CEViewService ceViewService;
  private CEReportScheduleService ceReportScheduleService;
  private ViewCustomFieldService viewCustomFieldService;
  private BigQueryService bigQueryService;
  private BigQueryHelper bigQueryHelper;

  @Inject
  public PerspectiveResource(CEViewService ceViewService, CEReportScheduleService ceReportScheduleService,
      ViewCustomFieldService viewCustomFieldService, BigQueryService bigQueryService, BigQueryHelper bigQueryHelper) {
    this.ceViewService = ceViewService;
    this.ceReportScheduleService = ceReportScheduleService;
    this.viewCustomFieldService = viewCustomFieldService;
    this.bigQueryService = bigQueryService;
    this.bigQueryHelper = bigQueryHelper;
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<CEView> create(@QueryParam("accountId") String accountId, @QueryParam("clone") boolean clone,
      @Valid @RequestBody CEView ceView) {
    ceView.setAccountId(accountId);
    if (clone) {
      // reset these fields which gets set downstream appropriately
      ceView.setCreatedBy(null);
      ceView.setCreatedAt(0);
    }
    return new RestResponse<>(updateTotalCost(ceViewService.save(ceView)));
  }

  private CEView updateTotalCost(CEView ceView) {
    BigQuery bigQuery = bigQueryService.get();
    String cloudProviderTableName = bigQueryHelper.getCloudProviderTableName(ceView.getAccountId(), UNIFIED_TABLE);
    return ceViewService.updateTotalCost(ceView, bigQuery, cloudProviderTableName);
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<CEView> get(
      @QueryParam("accountId") String accountId, @QueryParam("perspectiveId") String perspectiveId) {
    return new RestResponse<>(ceViewService.get(perspectiveId));
  }

  @PUT
  @Timed
  @ExceptionMetered
  public RestResponse<CEView> update(@QueryParam("accountId") String accountId, @Valid @RequestBody CEView ceView) {
    ceView.setAccountId(accountId);
    return new RestResponse<>(updateTotalCost(ceViewService.update(ceView)));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  public Response delete(@QueryParam("accountId") String accountId, @QueryParam("perspectiveId") String perspectiveId) {
    ceViewService.delete(perspectiveId, accountId);
    ceReportScheduleService.deleteAllByView(perspectiveId, accountId);
    viewCustomFieldService.deleteByViewId(perspectiveId, accountId);
    RestResponse rr = new RestResponse("Successfully deleted the view");
    return prepareResponse(rr, Response.Status.OK);
  }

  private Response prepareResponse(RestResponse restResponse, Response.Status status) {
    return Response.status(status).entity(restResponse).type(MediaType.APPLICATION_JSON).build();
  }
}
