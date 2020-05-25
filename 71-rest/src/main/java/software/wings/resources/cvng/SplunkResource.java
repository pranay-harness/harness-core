package software.wings.resources.cvng;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.splunk.SplunkSavedSearch;
import software.wings.service.intfc.splunk.SplunkAnalysisService;

import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("cv-nextgen/splunk/")
@Path("/cv-nextgen/splunk")
@Produces("application/json")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class SplunkResource {
  @Inject SplunkAnalysisService splunkAnalysisService;
  @GET
  @Path("get-saved-searches")
  @Timed
  @ExceptionMetered
  public RestResponse<List<SplunkSavedSearch>> getSavedSearches(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("connectorId") String connectorId) {
    return new RestResponse<>(splunkAnalysisService.getSavedSearches(accountId, connectorId));
  }
}