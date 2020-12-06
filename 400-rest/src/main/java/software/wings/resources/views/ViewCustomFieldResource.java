package software.wings.resources.views;

import static software.wings.graphql.datafetcher.billing.CloudBillingHelper.unified;

import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.views.entities.ViewCustomField;
import io.harness.ccm.views.graphql.CustomFieldExpressionHelper;
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

@Api("view-custom-field")
@Path("/view-custom-field")
@Produces("application/json")
public class ViewCustomFieldResource {
  private ViewCustomFieldService viewCustomFieldService;
  private CustomFieldExpressionHelper customFieldExpressionHelper;
  private BigQueryService bigQueryService;
  private CloudBillingHelper cloudBillingHelper;

  @Inject
  public ViewCustomFieldResource(ViewCustomFieldService viewCustomFieldService,
      CustomFieldExpressionHelper customFieldExpressionHelper, BigQueryService bigQueryService,
      CloudBillingHelper cloudBillingHelper) {
    this.viewCustomFieldService = viewCustomFieldService;
    this.customFieldExpressionHelper = customFieldExpressionHelper;
    this.bigQueryService = bigQueryService;
    this.cloudBillingHelper = cloudBillingHelper;
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<ViewCustomField> saveCustomField(
      @QueryParam("accountId") String accountId, ViewCustomField viewCustomField) {
    viewCustomField.setSqlFormula(customFieldExpressionHelper.getSQLFormula(
        viewCustomField.getUserDefinedExpression(), viewCustomField.getViewFields()));
    BigQuery bigQuery = bigQueryService.get();
    String cloudProviderTableName = cloudBillingHelper.getCloudProviderTableName(accountId, unified);
    return new RestResponse<>(viewCustomFieldService.save(viewCustomField, bigQuery, cloudProviderTableName));
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<ViewCustomField> get(
      @QueryParam("accountId") String accountId, @QueryParam("customFieldId") String customFieldId) {
    return new RestResponse<>(viewCustomFieldService.get(customFieldId));
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/validate")
  public Response validate(@QueryParam("accountId") String accountId, ViewCustomField viewCustomField) {
    viewCustomField.setSqlFormula(customFieldExpressionHelper.getSQLFormula(
        viewCustomField.getUserDefinedExpression(), viewCustomField.getViewFields()));
    BigQuery bigQuery = bigQueryService.get();
    String cloudProviderTableName = cloudBillingHelper.getCloudProviderTableName(accountId, unified);
    viewCustomFieldService.validate(viewCustomField, bigQuery, cloudProviderTableName);
    RestResponse rr = new RestResponse("Valid Formula");
    return prepareResponse(rr, Response.Status.OK);
  }

  @PUT
  @Timed
  @ExceptionMetered
  public RestResponse<ViewCustomField> update(
      @QueryParam("accountId") String accountId, @Valid @RequestBody ViewCustomField viewCustomField) {
    viewCustomField.setAccountId(accountId);
    viewCustomField.setSqlFormula(customFieldExpressionHelper.getSQLFormula(
        viewCustomField.getUserDefinedExpression(), viewCustomField.getViewFields()));
    BigQuery bigQuery = bigQueryService.get();
    String cloudProviderTableName = cloudBillingHelper.getCloudProviderTableName(accountId, unified);
    return new RestResponse<>(viewCustomFieldService.update(viewCustomField, bigQuery, cloudProviderTableName));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  public Response delete(@QueryParam("accountId") String accountId, @QueryParam("customFieldId") String customFieldId) {
    viewCustomFieldService.delete(customFieldId, accountId);
    RestResponse rr = new RestResponse("Successfully deleted the view");
    return prepareResponse(rr, Response.Status.OK);
  }

  private Response prepareResponse(RestResponse restResponse, Response.Status status) {
    return Response.status(status).entity(restResponse).type(MediaType.APPLICATION_JSON).build();
  }
}
