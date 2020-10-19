package software.wings.resources.views;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.ccm.views.entities.ViewCustomField;
import io.harness.ccm.views.graphql.CustomFieldExpressionHelper;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestBody;

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

@Api("view-custom-field")
@Path("/view-custom-field")
@Produces("application/json")
public class ViewCustomFieldResource {
  private ViewCustomFieldService viewCustomFieldService;
  private CustomFieldExpressionHelper customFieldExpressionHelper;

  @Inject
  public ViewCustomFieldResource(
      ViewCustomFieldService viewCustomFieldService, CustomFieldExpressionHelper customFieldExpressionHelper) {
    this.viewCustomFieldService = viewCustomFieldService;
    this.customFieldExpressionHelper = customFieldExpressionHelper;
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<ViewCustomField> saveCustomField(
      @QueryParam("accountId") String accountId, ViewCustomField viewCustomField) {
    viewCustomField.setSqlFormula(customFieldExpressionHelper.getSQLFormula(
        viewCustomField.getUserDefinedExpression(), viewCustomField.getViewFields()));
    return new RestResponse<>(viewCustomFieldService.save(viewCustomField));
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<ViewCustomField> get(
      @QueryParam("accountId") String accountId, @QueryParam("customFieldId") String customFieldId) {
    return new RestResponse<>(viewCustomFieldService.get(customFieldId));
  }

  @PUT
  @Timed
  @ExceptionMetered
  public RestResponse<ViewCustomField> update(
      @QueryParam("accountId") String accountId, @Valid @RequestBody ViewCustomField viewCustomField) {
    viewCustomField.setAccountId(accountId);
    viewCustomField.setSqlFormula(customFieldExpressionHelper.getSQLFormula(
        viewCustomField.getUserDefinedExpression(), viewCustomField.getViewFields()));
    return new RestResponse<>(viewCustomFieldService.update(viewCustomField));
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
