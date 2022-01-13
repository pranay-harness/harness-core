/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.analyserservice;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.QueryAlertCategory;
import io.harness.event.QueryStats;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.serviceinfo.ServiceInfo;
import io.harness.serviceinfo.ServiceInfoService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Api("analyser")
@Path("analyser")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class AnalyserResource {
  AnalyserService analyserService;
  ServiceInfoService serviceInfoService;

  @GET
  @Path("/stats")
  @ApiOperation(value = "get exhaustive set of queries by service and version", nickname = "getQueryStats")
  public ResponseDTO<List<QueryStats>> getQueryStats(
      @NotNull @QueryParam(AnalyserServiceConstants.SERVICE) String service,
      @NotNull @QueryParam(AnalyserServiceConstants.VERSION) String version) {
    return ResponseDTO.newResponse(analyserService.getQueryStats(service, version));
  }

  @GET
  @Path("/expensivequeries")
  @ApiOperation(value = "get most expensive queries", nickname = "getExpensiveQueries")
  public ResponseDTO<List<QueryStats>> getExpensiveQueries(
      @NotNull @QueryParam(AnalyserServiceConstants.SERVICE) String service,
      @NotNull @QueryParam(AnalyserServiceConstants.VERSION) String version) {
    return ResponseDTO.newResponse(analyserService.getMostExpensiveQueries(service, version));
  }

  @GET
  @Path("/querydiff")
  @ApiOperation(value = "get query diff among tow versions", nickname = "getDisjointQueries")
  public ResponseDTO<List<QueryStats>> getDisjointQueries(
      @NotNull @QueryParam(AnalyserServiceConstants.SERVICE) String service,
      @NotNull @QueryParam(AnalyserServiceConstants.OLD_VERSION) String oldVersion,
      @NotNull @QueryParam(AnalyserServiceConstants.NEW_VERSION) String newVersion) {
    return ResponseDTO.newResponse(analyserService.getDisjointQueries(service, oldVersion, newVersion));
  }

  @GET
  @Path("/alert")
  @ApiOperation(value = "get alerts of a service", nickname = "getAlerts")
  public ResponseDTO<List<QueryStats>> getAlerts(@NotNull @QueryParam(AnalyserServiceConstants.SERVICE) String service,
      @NotNull @QueryParam(AnalyserServiceConstants.VERSION) String version,
      @QueryParam(AnalyserServiceConstants.ALERT_TYPE) QueryAlertCategory alertType) {
    return ResponseDTO.newResponse(analyserService.getQueryStats(service, version, alertType));
  }

  @GET
  @Path("/alertcount")
  @ApiOperation(value = "get count of alerts for a service", nickname = "getAlertsCount")
  public ResponseDTO<Map<String, Integer>> getAlertsCount(
      @NotNull @QueryParam(AnalyserServiceConstants.SERVICE) String service,
      @QueryParam(AnalyserServiceConstants.ALERT_TYPE) QueryAlertCategory alertType) {
    return ResponseDTO.newResponse(analyserService.getAlertMap(service, alertType));
  }

  @GET
  @Path("/services")
  @ApiOperation(value = "get information for all services", nickname = "getServices")
  public ResponseDTO<List<ServiceInfo>> getAllServiceInfos() {
    return ResponseDTO.newResponse(serviceInfoService.getAllServices());
  }

  @GET
  @Path("/{service}/versions")
  @ApiOperation(value = "get information for all versions for a service", nickname = "getVersions")
  public ResponseDTO<List<String>> getAllVersions(
      @NotNull @PathParam(AnalyserServiceConstants.SERVICE) String service) {
    return ResponseDTO.newResponse(serviceInfoService.getAllVersions(service));
  }
}
