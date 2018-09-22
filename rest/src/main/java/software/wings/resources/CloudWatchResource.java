package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.cloudwatch.CloudWatchSetupTestNodeData;
import software.wings.service.intfc.CloudWatchService;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 5/3/18.
 */
@Api("cloudwatch")
@Path("/cloudwatch")
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class CloudWatchResource {
  @Inject private CloudWatchService cloudWatchService;

  @GET
  @Path("/get-metric-names")
  @Timed
  @ExceptionMetered
  public RestResponse<List<CloudWatchMetric>> getMetricNames(
      @QueryParam("accountId") final String accountId, @QueryParam("awsNameSpace") final AwsNameSpace awsNameSpace) {
    return new RestResponse<>(cloudWatchService.getCloudWatchMetrics().get(awsNameSpace));
  }

  @GET
  @Path("/get-load-balancers")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<String>> getLoadBalancerNames(@QueryParam("accountId") final String accountId,
      @QueryParam("settingId") final String settingId, @QueryParam("region") final String region) {
    return new RestResponse<>(cloudWatchService.getLoadBalancerNames(settingId, region));
  }

  /**
   * Api to fetch Metric data for given node.
   * @param accountId
   * @param setupTestNodeData
   * @return
   */
  @POST
  @Path("/node-data")
  @Timed
  @AuthRule(permissionType = PermissionType.WORKFLOW, action = Action.READ)
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      @QueryParam("accountId") final String accountId, @Valid CloudWatchSetupTestNodeData setupTestNodeData) {
    return new RestResponse<>(cloudWatchService.getMetricsWithDataForNode(setupTestNodeData));
  }
}
