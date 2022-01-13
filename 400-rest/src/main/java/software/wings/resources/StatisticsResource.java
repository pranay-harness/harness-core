/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.rest.RestResponse;

import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.ServiceInstanceStatistics;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.StatisticsService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 8/15/16.
 */
@Api("/statistics")
@Path("/statistics")
@Produces("application/json")
@Scope(ResourceType.APPLICATION)
public class StatisticsResource {
  @Inject private StatisticsService statisticsService;

  @GET
  @Path("deployment-stats")
  @Timed
  @ExceptionMetered
  public RestResponse<DeploymentStatistics> deploymentStats(@QueryParam("accountId") String accountId,
      @DefaultValue("30") @QueryParam("numOfDays") Integer numOfDays, @QueryParam("appId") List<String> appIds) {
    return new RestResponse<>(statisticsService.getDeploymentStatistics(accountId, appIds, numOfDays));
  }

  @GET
  @Path("service-instance-stats")
  @Timed
  @ExceptionMetered
  public RestResponse<ServiceInstanceStatistics> instanceStats(@QueryParam("accountId") String accountId,
      @DefaultValue("30") @QueryParam("numOfDays") Integer numOfDays, @QueryParam("appId") List<String> appIds) {
    return new RestResponse<>(statisticsService.getServiceInstanceStatistics(accountId, appIds, numOfDays));
  }
}
