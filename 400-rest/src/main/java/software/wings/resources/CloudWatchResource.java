/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.rest.RestResponse;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.cloudwatch.CloudWatchSetupTestNodeData;
import software.wings.service.intfc.CloudWatchService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import java.util.Map;
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
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      @QueryParam("accountId") final String accountId, @Valid CloudWatchSetupTestNodeData setupTestNodeData) {
    return new RestResponse<>(cloudWatchService.getMetricsWithDataForNode(setupTestNodeData));
  }

  @GET
  @Path("/get-lambda-functions")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getLambdaFunctionsNames(@QueryParam("accountId") final String accountId,
      @QueryParam("settingId") final String settingId, @QueryParam("region") final String region) {
    return new RestResponse<>(cloudWatchService.getLambdaFunctionsNames(settingId, region));
  }

  @GET
  @Path("/get-ec2-instances")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getEC2Instances(@QueryParam("accountId") final String accountId,
      @QueryParam("settingId") final String settingId, @QueryParam("region") final String region) {
    return new RestResponse<>(cloudWatchService.getEC2Instances(settingId, region));
  }

  @GET
  @Path("/get-ecs-cluster-names")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getECSClusterNames(@QueryParam("accountId") final String accountId,
      @QueryParam("settingId") final String settingId, @QueryParam("region") final String region) {
    return new RestResponse<>(cloudWatchService.getECSClusterNames(settingId, region));
  }
}
