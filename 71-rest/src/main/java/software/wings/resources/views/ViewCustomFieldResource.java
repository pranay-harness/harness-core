package software.wings.resources.views;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.ccm.views.entities.ViewCustomField;
import io.harness.ccm.views.graphql.CustomFieldExpressionHelper;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

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
}
