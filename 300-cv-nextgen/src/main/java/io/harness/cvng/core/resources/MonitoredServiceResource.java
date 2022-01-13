/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.resources;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.MonitoredServiceType;
import io.harness.cvng.beans.change.ChangeCategory;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.core.beans.HealthMonitoringFlagResponse;
import io.harness.cvng.core.beans.change.ChangeSummaryDTO;
import io.harness.cvng.core.beans.monitoredService.AnomaliesSummaryDTO;
import io.harness.cvng.core.beans.monitoredService.CountServiceDTO;
import io.harness.cvng.core.beans.monitoredService.DurationDTO;
import io.harness.cvng.core.beans.monitoredService.HealthScoreDTO;
import io.harness.cvng.core.beans.monitoredService.HistoricalTrend;
import io.harness.cvng.core.beans.monitoredService.MetricDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceListItemDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceResponse;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceWithHealthSources;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceDTO;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.beans.params.TimeRangeParams;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponse;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.sun.istack.internal.NotNull;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.time.Instant;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;

@Api("monitored-service")
@Path("monitored-service")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
@OwnedBy(HarnessTeam.CV)
public class MonitoredServiceResource {
  @Inject MonitoredServiceService monitoredServiceService;
  @Inject AccessControlClient accessControlClient;

  public static final String MONITORED_SERVICE = "MONITOREDSERVICE";
  public static final String EDIT_PERMISSION = "chi_monitoredservice_edit";
  public static final String VIEW_PERMISSION = "chi_monitoredservice_view";
  public static final String TOGGLE_PERMISSION = "chi_monitoredservice_toggle";
  public static final String DELETE_PERMISSION = "chi_monitoredservice_delete";

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "saves monitored service data", nickname = "saveMonitoredService")
  public RestResponse<MonitoredServiceResponse> saveMonitoredService(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @NotNull @Valid @Body MonitoredServiceDTO monitoredServiceDTO) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountId, monitoredServiceDTO.getOrgIdentifier(), monitoredServiceDTO.getProjectIdentifier()),
        Resource.of(MONITORED_SERVICE, null), EDIT_PERMISSION);
    return new RestResponse<>(monitoredServiceService.create(accountId, monitoredServiceDTO));
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/create-default")
  @ApiOperation(value = "created default monitored service", nickname = "createDefaultMonitoredService")
  public RestResponse<MonitoredServiceResponse> createDefaultMonitoredService(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("environmentIdentifier") String environmentIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("serviceIdentifier") String serviceIdentifier) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return new RestResponse<>(
        monitoredServiceService.createDefault(projectParams, serviceIdentifier, environmentIdentifier));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @Path("{identifier}")
  @ApiOperation(value = "updates monitored service data", nickname = "updateMonitoredService")
  public RestResponse<MonitoredServiceResponse> updateMonitoredService(
      @ApiParam(required = true) @NotNull @PathParam("identifier") String identifier,
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @NotNull @Valid @Body MonitoredServiceDTO monitoredServiceDTO) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountId, monitoredServiceDTO.getOrgIdentifier(), monitoredServiceDTO.getProjectIdentifier()),
        Resource.of(MONITORED_SERVICE, identifier), EDIT_PERMISSION);
    Preconditions.checkArgument(identifier.equals(monitoredServiceDTO.getIdentifier()),
        String.format(
            "Identifier %s does not match with path identifier %s", monitoredServiceDTO.getIdentifier(), identifier));
    return new RestResponse<>(monitoredServiceService.update(accountId, monitoredServiceDTO));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @Path("{identifier}/health-monitoring-flag")
  @ApiOperation(value = "updates monitored service data", nickname = "setHealthMonitoringFlag")
  @NGAccessControlCheck(resourceType = MONITORED_SERVICE, permission = TOGGLE_PERMISSION)
  public RestResponse<HealthMonitoringFlagResponse> setHealthMonitoringFlag(
      @NotNull @PathParam("identifier") @ResourceIdentifier String identifier,
      @NotNull @QueryParam("accountId") @AccountIdentifier String accountId,
      @NotNull @QueryParam("orgIdentifier") @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam("enable") Boolean enable) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return new RestResponse<>(monitoredServiceService.setHealthMonitoringFlag(projectParams, identifier, enable));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("{identifier}/overall-health-score")
  @ApiOperation(
      value = "get monitored service overall health score data ", nickname = "getMonitoredServiceOverAllHealthScore")
  public ResponseDTO<HistoricalTrend>
  getOverAllHealthScore(@NotNull @NotEmpty @PathParam("identifier") String identifier,
      @NotNull @QueryParam("accountId") String accountId, @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("duration") DurationDTO durationDTO, @NotNull @QueryParam("endTime") Long endTime) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return ResponseDTO.newResponse(monitoredServiceService.getOverAllHealthScore(
        projectParams, identifier, durationDTO, Instant.ofEpochMilli(endTime)));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("overall-health-score")
  @ApiOperation(value = "get monitored service overall health score data using service and environment identifiers",
      nickname = "getMonitoredServiceOverAllHealthScoreWithServiceAndEnv")
  public ResponseDTO<HistoricalTrend>
  getOverAllHealthScore(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("environmentIdentifier") String environmentIdentifier,
      @NotNull @QueryParam("serviceIdentifier") String serviceIdentifier,
      @NotNull @QueryParam("duration") DurationDTO durationDTO, @NotNull @QueryParam("endTime") Long endTime) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .serviceIdentifier(serviceIdentifier)
                                                            .environmentIdentifier(environmentIdentifier)
                                                            .accountIdentifier(accountId)
                                                            .orgIdentifier(orgIdentifier)
                                                            .projectIdentifier(projectIdentifier)
                                                            .build();
    return ResponseDTO.newResponse(monitoredServiceService.getOverAllHealthScore(
        serviceEnvironmentParams, durationDTO, Instant.ofEpochMilli(endTime)));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "list monitored service data ", nickname = "listMonitoredService")
  @NGAccessControlCheck(resourceType = MONITORED_SERVICE, permission = VIEW_PERMISSION)
  public ResponseDTO<PageResponse<MonitoredServiceListItemDTO>> list(
      @NotNull @QueryParam("accountId") @AccountIdentifier String accountId,
      @NotNull @QueryParam("orgIdentifier") @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") @ProjectIdentifier String projectIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier, @QueryParam("offset") @NotNull Integer offset,
      @QueryParam("pageSize") @NotNull Integer pageSize, @QueryParam("filter") String filter,
      @NotNull @QueryParam("servicesAtRiskFilter") @ApiParam(defaultValue = "false") boolean servicesAtRiskFilter) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return ResponseDTO.newResponse(monitoredServiceService.list(
        projectParams, environmentIdentifier, offset, pageSize, filter, servicesAtRiskFilter));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("{identifier}")
  @ApiOperation(value = "get monitored service data ", nickname = "getMonitoredService")
  @NGAccessControlCheck(resourceType = MONITORED_SERVICE, permission = VIEW_PERMISSION)
  public ResponseDTO<MonitoredServiceResponse> get(
      @NotNull @PathParam("identifier") @ResourceIdentifier String identifier,
      @NotNull @QueryParam("accountId") @AccountIdentifier String accountId,
      @NotNull @QueryParam("orgIdentifier") @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") @ProjectIdentifier String projectIdentifier) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return ResponseDTO.newResponse(monitoredServiceService.get(projectParams, identifier));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/list")
  @ApiOperation(value = "get list of monitored service data ", nickname = "getMonitoredServiceList")
  public ResponseDTO<PageResponse<MonitoredServiceResponse>> getList(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("environmentIdentifier") String environmentIdentifier,
      @QueryParam("offset") @NotNull Integer offset, @QueryParam("pageSize") @NotNull Integer pageSize,
      @QueryParam("filter") String filter) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return ResponseDTO.newResponse(
        monitoredServiceService.getList(projectParams, environmentIdentifier, offset, pageSize, filter));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/all/time-series-health-sources")
  @ApiOperation(value = "get all of monitored service data with time series health sources ",
      nickname = "getAllMonitoredServicesWithTimeSeriesHealthSources")
  public ResponseDTO<List<MonitoredServiceWithHealthSources>>
  getAllMonitoredServicesWithHealthSources(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return ResponseDTO.newResponse(monitoredServiceService.getAllWithTimeSeriesHealthSources(projectParams));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/service-environment")
  @ApiOperation(value = "get monitored service data from service and env ref",
      nickname = "getMonitoredServiceFromServiceAndEnvironment")
  public ResponseDTO<MonitoredServiceResponse>
  getMonitoredServiceFromServiceAndEnvironment(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("serviceIdentifier") String serviceIdentifier,
      @NotNull @QueryParam("environmentIdentifier") String environmentIdentifier) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .serviceIdentifier(serviceIdentifier)
                                                            .environmentIdentifier(environmentIdentifier)
                                                            .accountIdentifier(accountId)
                                                            .orgIdentifier(orgIdentifier)
                                                            .projectIdentifier(projectIdentifier)
                                                            .build();
    return ResponseDTO.newResponse(monitoredServiceService.get(serviceEnvironmentParams));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/scores")
  @ApiOperation(value = "get monitored service scores from service and env ref",
      nickname = "getMonitoredServiceScoresFromServiceAndEnvironment")
  public ResponseDTO<HealthScoreDTO>
  getMonitoredServiceScoreFromServiceAndEnvironment(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("serviceIdentifier") String serviceIdentifier,
      @NotNull @QueryParam("environmentIdentifier") String environmentIdentifier) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(accountId)
                                                            .orgIdentifier(orgIdentifier)
                                                            .projectIdentifier(projectIdentifier)
                                                            .serviceIdentifier(serviceIdentifier)
                                                            .environmentIdentifier(environmentIdentifier)
                                                            .build();
    return ResponseDTO.newResponse(
        monitoredServiceService.getCurrentAndDependentServicesScore(serviceEnvironmentParams));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @Path("{identifier}")
  @ApiOperation(value = "delete monitored service data ", nickname = "deleteMonitoredService")
  @NGAccessControlCheck(resourceType = MONITORED_SERVICE, permission = DELETE_PERMISSION)
  public RestResponse<Boolean> delete(
      @ApiParam(required = true) @NotNull @PathParam("identifier") @ResourceIdentifier String identifier,
      @ApiParam(required = true) @NotNull @QueryParam("accountId") @AccountIdentifier String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") @OrgIdentifier String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam(
          "projectIdentifier") @ProjectIdentifier String projectIdentifier) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return new RestResponse<>(monitoredServiceService.delete(projectParams, identifier));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/environments")
  @ApiOperation(
      value = "get monitored service list environments data ", nickname = "getMonitoredServiceListEnvironments")
  public ResponseDTO<List<EnvironmentResponse>>
  getEnvironments(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier) {
    return ResponseDTO.newResponse(
        monitoredServiceService.listEnvironments(accountId, orgIdentifier, projectIdentifier));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/yaml-template")
  @ApiOperation(value = "yaml template for monitored service", nickname = "getMonitoredServiceYamlTemplate")
  @NGAccessControlCheck(resourceType = MONITORED_SERVICE, permission = VIEW_PERMISSION)
  public RestResponse<String> yamlTemplate(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") @AccountIdentifier String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") @OrgIdentifier String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") @ProjectIdentifier String projectIdentifier,
      @ApiParam @QueryParam("type") MonitoredServiceType type) {
    return new RestResponse<>(monitoredServiceService.getYamlTemplate(ProjectParams.builder()
                                                                          .accountIdentifier(accountId)
                                                                          .orgIdentifier(orgIdentifier)
                                                                          .projectIdentifier(projectIdentifier)
                                                                          .build(),
        type));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/health-sources")
  @ApiOperation(value = "get all health sources for service and environment",
      nickname = "getAllHealthSourcesForServiceAndEnvironment")
  public RestResponse<List<HealthSourceDTO>>
  getHealthSources(@ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("serviceIdentifier") String serviceIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("environmentIdentifier") String environmentIdentifier) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(accountId)
                                                            .orgIdentifier(orgIdentifier)
                                                            .projectIdentifier(projectIdentifier)
                                                            .serviceIdentifier(serviceIdentifier)
                                                            .environmentIdentifier(environmentIdentifier)
                                                            .build();

    return new RestResponse<>(monitoredServiceService.getHealthSources(serviceEnvironmentParams));
  }

  @GET
  @Timed
  @Path("{identifier}/change-event")
  @ExceptionMetered
  @ApiOperation(value = "get ChangeEvent List", nickname = "getChangeEventList")
  public RestResponse<List<ChangeEventDTO>> get(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @PathParam("identifier") String identifier,
      @ApiParam(required = true) @NotNull @QueryParam("startTime") long startTime,
      @ApiParam(required = true) @NotNull @QueryParam("endTime") long endTime,
      @ApiParam @NotNull @QueryParam("changeCategories") List<ChangeCategory> changeCategories) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return new RestResponse<>(monitoredServiceService.getChangeEvents(
        projectParams, identifier, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), changeCategories));
  }

  @GET
  @Timed
  @Path("{identifier}/change-event/summary")
  @ExceptionMetered
  @ApiOperation(value = "get ChangeEvent summary", nickname = "getChangeSummary")
  public RestResponse<ChangeSummaryDTO> get(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @PathParam("identifier") String identifier,
      @ApiParam(required = true) @NotNull @QueryParam("startTime") long startTime,
      @ApiParam(required = true) @NotNull @QueryParam("endTime") long endTime) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return new RestResponse<>(monitoredServiceService.getChangeSummary(
        projectParams, identifier, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime)));
  }

  @GET
  @Timed
  @Path("{identifier}/anomaliesCount")
  @ExceptionMetered
  @ApiOperation(value = "get anomalies summary details", nickname = "getAnomaliesSummary")
  public RestResponse<AnomaliesSummaryDTO> getAnomaliesSummary(
      @ApiParam(required = true) @NotNull @QueryParam("accountId") String accountId,
      @ApiParam(required = true) @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @ApiParam(required = true) @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @PathParam("identifier") String identifier,
      @ApiParam(required = true) @NotNull @QueryParam("startTime") long startTime,
      @ApiParam(required = true) @NotNull @QueryParam("endTime") long endTime) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    TimeRangeParams timeRangeParams = TimeRangeParams.builder()
                                          .startTime(Instant.ofEpochMilli(startTime))
                                          .endTime(Instant.ofEpochMilli(endTime))
                                          .build();
    return new RestResponse<>(monitoredServiceService.getAnomaliesSummary(projectParams, identifier, timeRangeParams));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/count-of-services")
  @ApiOperation(value = "get count of types of services like Monitored Service, Services at Risk ",
      nickname = "getCountOfServices")
  public CountServiceDTO
  getCountOfServices(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier, @QueryParam("filter") String filter) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return monitoredServiceService.getCountOfServices(projectParams, environmentIdentifier, filter);
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/{monitoredServiceIdentifier}/health-source/{healthSourceIdentifier}/slo-metrics")
  @ApiOperation(value = "get slo metrics in a healthSource ", nickname = "getSloMetrcs")
  public RestResponse<List<MetricDTO>> getSloMetrics(@BeanParam ProjectParams projectParams,
      @PathParam("monitoredServiceIdentifier") String monitoredServiceIdentifier,
      @PathParam("healthSourceIdentifier") String healthSourceIdentifier) {
    return new RestResponse<>(
        monitoredServiceService.getSloMetrics(projectParams, monitoredServiceIdentifier, healthSourceIdentifier));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/service-details")
  @ApiOperation(value = "get details of a monitored service present in the Service Dependency Graph",
      nickname = "getMonitoredServiceDetails")
  public MonitoredServiceListItemDTO
  getMonitoredServiceDetails(@BeanParam ServiceEnvironmentParams serviceEnvironmentParams) {
    return monitoredServiceService.getMonitoredServiceDetails(serviceEnvironmentParams);
  }
}
