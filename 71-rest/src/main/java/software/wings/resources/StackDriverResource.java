package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.stackdriver.StackDriverMetric;
import software.wings.service.impl.stackdriver.StackDriverSetupTestNodeData;
import software.wings.service.intfc.stackdriver.StackDriverService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by Pranjal on 11/27/2018
 */
@Api("stackdriver")
@Path("/stackdriver")
@Produces("application/json")
@Scope(SETTING)
public class StackDriverResource {
  @Inject private StackDriverService stackDriverService;

  @GET
  @Path("/get-metric-names")
  @Timed
  @ExceptionMetered
  public RestResponse<List<StackDriverMetric>> getMetricNames(
      @QueryParam("accountId") final String accountId, @QueryParam("namespace") final String nameSpace) {
    return new RestResponse<>(stackDriverService.getMetrics().get(nameSpace));
  }

  /**
   * Api to fetch Metric data for given node.
   * @param accountId
   * @param stackDriverSetupTestNodeData
   * @return
   */
  @POST
  @Path("/node-data")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getDataForNode(@QueryParam("accountId") final String accountId,
      @Valid StackDriverSetupTestNodeData stackDriverSetupTestNodeData) throws IOException {
    return new RestResponse<>(stackDriverService.getDataForNode(stackDriverSetupTestNodeData));
  }

  @GET
  @Path("/get-regions")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getRegions(@QueryParam("accountId") final String accountId,
      @QueryParam("settingId") final String settingId) throws IOException {
    return new RestResponse<>(stackDriverService.listRegions(settingId));
  }

  @GET
  @Path("/get-load-balancers")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getLoadBalancers(@QueryParam("accountId") final String accountId,
      @QueryParam("settingId") final String settingId, @QueryParam("region") final String region) throws IOException {
    return new RestResponse<>(stackDriverService.listForwardingRules(settingId, region));
  }
}
