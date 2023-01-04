/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.change.ChangeCategory;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.core.beans.change.ChangeSummaryDTO;
import io.harness.cvng.core.beans.change.ChangeTimeline;
import io.harness.cvng.core.beans.params.ProjectPathParams;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("change-event")
@OwnedBy(HarnessTeam.CV)
@Produces("application/json")
@ExposeInternalException
public interface ChangeEventNgResource {
  @GET
  @Timed
  @NextGenManagerAuth
  @ExceptionMetered
  @Path("")
  @ApiOperation(value = "get ChangeEvent List", nickname = "changeEventList")
  RestResponse<PageResponse<ChangeEventDTO>> get(@Valid @BeanParam ProjectPathParams projectPathParams,
      @QueryParam("serviceIdentifiers") List<String> serviceIdentifiers,
      @QueryParam("envIdentifiers") List<String> envIdentifiers,
      @QueryParam("monitoredServiceIdentifiers") List<String> monitoredServiceIdentifiers,
      @QueryParam("isMonitoredServiceIdentifierScoped") boolean isMonitoredServiceIdentifierScoped,
      @QueryParam("changeCategories") List<ChangeCategory> changeCategories,
      @QueryParam("changeSourceTypes") List<ChangeSourceType> changeSourceTypes,
      @QueryParam("searchText") String searchText,
      @ApiParam(required = true) @NotNull @QueryParam("st"
          + "artTime") long startTime,
      @ApiParam(required = true) @NotNull @QueryParam("endTime") long endTime, @BeanParam PageRequest pageRequest);

  @GET
  @Timed
  @NextGenManagerAuth
  @Path("/timeline")
  @ExceptionMetered
  @ApiOperation(value = "get ChangeEvent timeline", nickname = "changeEventTimeline")
  RestResponse<ChangeTimeline> get(@Valid @BeanParam ProjectPathParams projectPathParams,
      @QueryParam("serviceIdentifiers") List<String> serviceIdentifiers,
      @QueryParam("envIdentifiers") List<String> envIdentifiers,
      @QueryParam("monitoredServiceIdentifiers") List<String> monitoredServiceIdentifiers,
      @QueryParam("isMonitoredServiceIdentifierScoped") boolean isMonitoredServiceIdentifierScoped,
      @QueryParam("changeCategories") List<ChangeCategory> changeCategories,
      @QueryParam("changeSourceTypes") List<ChangeSourceType> changeSourceTypes,
      @QueryParam("searchText") String searchText,
      @ApiParam(required = true) @NotNull @QueryParam("startTime") long startTime,
      @ApiParam(required = true) @NotNull @QueryParam("endTime") long endTime,
      @ApiParam @QueryParam("pointCount") @DefaultValue("48") Integer pointCount);

  @GET
  @Timed
  @NextGenManagerAuth
  @Path("/monitored-service-summary")
  @ExceptionMetered
  @ApiOperation(
      value = "get ChangeEvent summary for monitored service", nickname = "getMonitoredServiceChangeEventSummary")
  RestResponse<ChangeSummaryDTO>
  getSummary(@Valid @BeanParam ProjectPathParams projectPathParams,
      @QueryParam("monitoredServiceIdentifier") String monitoredServiceIdentifier,
      @QueryParam("monitoredServiceIdentifiers") List<String> monitoredServiceIdentifiers,
      @QueryParam("isMonitoredServiceIdentifierScoped") boolean isMonitoredServiceIdentifierScoped,
      @QueryParam("changeCategories") List<ChangeCategory> changeCategories,
      @QueryParam("changeSourceTypes") List<ChangeSourceType> changeSourceTypes,
      @ApiParam(required = true) @NotNull @QueryParam("startTime") long startTime,
      @ApiParam(required = true) @NotNull @QueryParam("endTime") long endTime);
}
