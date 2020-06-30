package software.wings.resources.cvng;

import static io.harness.cvng.core.services.CVNextGenConstants.SPLUNK_HISTOGRAM_PATH;
import static io.harness.cvng.core.services.CVNextGenConstants.SPLUNK_RESOURCE_PATH;
import static io.harness.cvng.core.services.CVNextGenConstants.SPLUNK_SAMPLE_PATH;
import static io.harness.cvng.core.services.CVNextGenConstants.SPLUNK_SAVED_SEARCH_PATH;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.CVHistogram;
import io.harness.cvng.beans.SplunkSampleResponse;
import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.swagger.annotations.Api;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.splunk.SplunkAnalysisService;

import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.GET;
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
  @GET
  @Path(SPLUNK_SAVED_SEARCH_PATH)
  @Timed
  @ExceptionMetered
  public RestResponse<List<SplunkSavedSearch>> getSavedSearches(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("connectorId") String connectorId) {
    return new RestResponse<>(splunkAnalysisService.getSavedSearches(accountId, connectorId));
  }

  @GET
  @Path(SPLUNK_HISTOGRAM_PATH)
  @Timed
  @ExceptionMetered
  public RestResponse<CVHistogram> getHistogram(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorId") String connectorId, @QueryParam("query") String query) {
    return new RestResponse<>(splunkAnalysisService.getHistogram(accountId, connectorId, query));
  }

  @GET
  @Path(SPLUNK_SAMPLE_PATH)
  @Timed
  @ExceptionMetered
  public RestResponse<SplunkSampleResponse> getSamples(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorId") String connectorId, @QueryParam("query") String query) {
    return new RestResponse<>(splunkAnalysisService.getSamples(accountId, connectorId, query));
  }
}