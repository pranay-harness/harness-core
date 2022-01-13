/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.cvng;

import static io.harness.cvng.core.services.CVNextGenConstants.SPLUNK_RESOURCE_PATH;
import static io.harness.cvng.core.services.CVNextGenConstants.SPLUNK_SAVED_SEARCH_PATH;
import static io.harness.cvng.core.services.CVNextGenConstants.SPLUNK_VALIDATION_RESPONSE_PATH;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.SplunkValidationResponse;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.splunk.SplunkAnalysisService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(SPLUNK_RESOURCE_PATH)
@Path(SPLUNK_RESOURCE_PATH)
@Produces("application/json")
@Scope(PermissionAttribute.ResourceType.SETTING)
@ExposeInternalException(withStackTrace = true)
@LearningEngineAuth
public class SplunkResource {
  @Inject private SplunkAnalysisService splunkAnalysisService;
  @POST
  @Path(SPLUNK_SAVED_SEARCH_PATH)
  @Timed
  @ExceptionMetered
  public RestResponse<List<SplunkSavedSearch>> getSavedSearches(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorId") String connectorId, @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("requestGuid") @NotNull String requestGuid, SplunkConnectorDTO splunkConnectorDTO) {
    return new RestResponse<>(
        splunkAnalysisService.getSavedSearches(splunkConnectorDTO, orgIdentifier, projectIdentifier, requestGuid));
  }

  @POST
  @Path(SPLUNK_VALIDATION_RESPONSE_PATH)
  @Timed
  @ExceptionMetered
  public RestResponse<SplunkValidationResponse> getValidationResponse(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("connectorId") String connectorId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier, @QueryParam("query") String query,
      @QueryParam("requestGuid") @NotNull String requestGuid, SplunkConnectorDTO splunkConnectorDTO) {
    return new RestResponse<>(splunkAnalysisService.getValidationResponse(
        splunkConnectorDTO, orgIdentifier, projectIdentifier, query, requestGuid));
  }
}
