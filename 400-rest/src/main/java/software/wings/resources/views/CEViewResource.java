package software.wings.resources.views;

import static software.wings.graphql.datafetcher.billing.CloudBillingHelper.unified;

import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.budget.BudgetService;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.service.CEReportScheduleService;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.rest.RestResponse;

import software.wings.graphql.datafetcher.billing.CloudBillingHelper;

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
import org.springframework.web.bind.annotation.RequestBody;

@Api("view")
@Path("/view")
@Produces("application/json")
public class CEViewResource {
  private CEViewService ceViewService;
  private CEReportScheduleService ceReportScheduleService;
  private ViewCustomFieldService viewCustomFieldService;
  private BigQueryService bigQueryService;
  private CloudBillingHelper cloudBillingHelper;
  private BudgetService budgetService;

  @Inject
  public CEViewResource(CEViewService ceViewService, CEReportScheduleService ceReportScheduleService,
      ViewCustomFieldService viewCustomFieldService, BigQueryService bigQueryService,
      CloudBillingHelper cloudBillingHelper, BudgetService budgetService) {
    this.ceViewService = ceViewService;
    this.ceReportScheduleService = ceReportScheduleService;
    this.viewCustomFieldService = viewCustomFieldService;
    this.bigQueryService = bigQueryService;
    this.cloudBillingHelper = cloudBillingHelper;
    this.budgetService = budgetService;
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
    String cloudProviderTableName = cloudBillingHelper.getCloudProviderTableName(ceView.getAccountId(), unified);
    return ceViewService.updateTotalCost(ceView, bigQuery, cloudProviderTableName);
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<CEView> get(@QueryParam("accountId") String accountId, @QueryParam("viewId") String viewId) {
    return new RestResponse<>(ceViewService.get(viewId));
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
  public Response delete(@QueryParam("accountId") String accountId, @QueryParam("viewId") String viewId) {
    ceViewService.delete(viewId, accountId);
    ceReportScheduleService.deleteAllByView(viewId, accountId);
    viewCustomFieldService.deleteByViewId(viewId, accountId);
    budgetService.deleteByViewId(viewId, accountId);
    RestResponse rr = new RestResponse("Successfully deleted the view");
    return prepareResponse(rr, Response.Status.OK);
  }

  private Response prepareResponse(RestResponse restResponse, Response.Status status) {
    return Response.status(status).entity(restResponse).type(MediaType.APPLICATION_JSON).build();
  }
}
