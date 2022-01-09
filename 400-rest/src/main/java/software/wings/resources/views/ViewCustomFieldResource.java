/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.views;

import static software.wings.graphql.datafetcher.billing.CloudBillingHelper.unified;

import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.views.dao.ViewCustomFieldDao;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewCustomField;
import io.harness.ccm.views.entities.ViewCustomFunction;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.graphql.CustomFieldExpressionHelper;
import io.harness.ccm.views.graphql.ViewsMetaDataFields;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.rest.RestResponse;

import software.wings.graphql.datafetcher.billing.CloudBillingHelper;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.cloud.bigquery.BigQuery;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private ViewCustomFieldDao customFieldDao;
  private static final String labelsSubQuery = "(SELECT value FROM UNNEST(labels) WHERE KEY='%s')";

  @Inject
  public ViewCustomFieldResource(ViewCustomFieldService viewCustomFieldService,
      CustomFieldExpressionHelper customFieldExpressionHelper, BigQueryService bigQueryService,
      CloudBillingHelper cloudBillingHelper, ViewCustomFieldDao customFieldDao) {
    this.viewCustomFieldService = viewCustomFieldService;
    this.customFieldExpressionHelper = customFieldExpressionHelper;
    this.bigQueryService = bigQueryService;
    this.cloudBillingHelper = cloudBillingHelper;
    this.customFieldDao = customFieldDao;
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<ViewCustomField> saveCustomField(
      @QueryParam("accountId") String accountId, ViewCustomField viewCustomField) {
    modifyCustomField(viewCustomField);
    BigQuery bigQuery = bigQueryService.get();
    String cloudProviderTableName = cloudBillingHelper.getCloudProviderTableName(accountId, unified);
    return new RestResponse<>(viewCustomFieldService.save(viewCustomField, bigQuery, cloudProviderTableName));
  }

  private void modifyCustomField(ViewCustomField viewCustomField) {
    List<ViewField> viewFields = new ArrayList<>();
    String customFieldDisplayFormula = viewCustomField.getDisplayFormula();

    Pattern customFieldPattern = Pattern.compile("CUSTOM.([^ ,)]*)");
    Matcher customFieldPatternMatcher = customFieldPattern.matcher(customFieldDisplayFormula);
    while (customFieldPatternMatcher.find()) {
      String customFieldName = customFieldPatternMatcher.group(1);
      ViewCustomField customFieldDaoByName =
          customFieldDao.findByName(viewCustomField.getAccountId(), viewCustomField.getViewId(), customFieldName);
      customFieldDisplayFormula = customFieldDisplayFormula.replaceAll(
          customFieldPatternMatcher.group(), customFieldDaoByName.getDisplayFormula());
    }
    String userDefinedExpression = customFieldDisplayFormula;

    Pattern labelsPattern = Pattern.compile("LABEL.([^ ,)]*)");
    Matcher labelsPatternMatcher = labelsPattern.matcher(userDefinedExpression);
    while (labelsPatternMatcher.find()) {
      String labelKey = labelsPatternMatcher.group(1);
      viewFields.add(ViewField.builder()
                         .fieldName(labelKey)
                         .fieldId(ViewsMetaDataFields.LABEL_KEY.getFieldName())
                         .identifier(ViewFieldIdentifier.LABEL)
                         .build());
      userDefinedExpression =
          userDefinedExpression.replaceAll(labelsPatternMatcher.group(), String.format(labelsSubQuery, labelKey));
    }

    HashMap<String, ViewField> viewFieldsHashMap = customFieldExpressionHelper.getViewFieldsHashMap();
    for (Map.Entry<String, ViewField> viewFieldEntry : viewFieldsHashMap.entrySet()) {
      String key = viewFieldEntry.getKey();
      if (customFieldDisplayFormula.contains(key)) {
        ViewField viewField = viewFieldEntry.getValue();
        viewFields.add(viewField);
        userDefinedExpression = userDefinedExpression.replaceAll(key, viewField.getFieldId());
      }
    }

    viewCustomField.setViewFields(viewFields);
    viewCustomField.setUserDefinedExpression(userDefinedExpression);
    viewCustomField.setSqlFormula(
        userDefinedExpression.replaceAll(ViewCustomFunction.ONE_OF.getName(), ViewCustomFunction.ONE_OF.getFormula()));
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
    modifyCustomField(viewCustomField);
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
    modifyCustomField(viewCustomField);
    BigQuery bigQuery = bigQueryService.get();
    String cloudProviderTableName = cloudBillingHelper.getCloudProviderTableName(accountId, unified);
    return new RestResponse<>(viewCustomFieldService.update(viewCustomField, bigQuery, cloudProviderTableName));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  public Response delete(@QueryParam("accountId") String accountId, @QueryParam("customFieldId") String customFieldId,
      @Valid @RequestBody CEView ceView) {
    viewCustomFieldService.delete(customFieldId, accountId, ceView);
    RestResponse rr = new RestResponse("Successfully deleted the view");
    return prepareResponse(rr, Response.Status.OK);
  }

  private Response prepareResponse(RestResponse restResponse, Response.Status status) {
    return Response.status(status).entity(restResponse).type(MediaType.APPLICATION_JSON).build();
  }
}
