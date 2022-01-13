/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.GE;
import static io.harness.beans.SearchFilter.Operator.LT_EQ;
import static io.harness.beans.SortOrder.Builder.aSortOrder;
import static io.harness.beans.SortOrder.OrderType.DESC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import static java.util.Arrays.asList;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SortOrder;
import io.harness.persistence.CreatedAtAware;
import io.harness.rest.RestResponse;

import software.wings.beans.Activity;
import software.wings.beans.Log;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.dl.WingsPersistence;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.GoogleDataStoreServiceImpl;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallLogKeys;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.LogService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Api("activities")
@Path("/activities")
@Produces("application/json")
@Scope(APPLICATION)
public class ActivityResource {
  private ActivityService activityService;
  private LogService logService;
  private DataStoreService dataStoreService;
  private WingsPersistence wingsPersistence;

  /**
   * Instantiates a new activity resource.
   *
   * @param activityService the activity service
   * @param logService      the log service
   */
  @Inject
  public ActivityResource(ActivityService activityService, LogService logService, DataStoreService dataStoreService,
      WingsPersistence wingsPersistence) {
    this.activityService = activityService;
    this.logService = logService;
    this.dataStoreService = dataStoreService;
    this.wingsPersistence = wingsPersistence;
  }

  /**
   * List.
   *
   * @param envId   the env id
   * @param request the request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Activity>> list(@QueryParam("accountId") String accountId,
      @QueryParam("envId") String envId, @BeanParam PageRequest<Activity> request) {
    if (isNotEmpty(envId)) {
      request.addFilter("environmentId", EQ, envId);
    }
    if (request.getPageSize() > ResourceConstants.DEFAULT_RUNTIME_ENTITY_PAGESIZE) {
      request.setLimit(ResourceConstants.DEFAULT_RUNTIME_ENTITY_PAGESIZE_STR);
    }
    return new RestResponse<>(activityService.list(request));
  }

  /**
   * Gets the.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @return the rest response
   */

  @GET
  @Path("{activityId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Activity> get(@QueryParam("appId") String appId, @PathParam("activityId") String activityId) {
    return new RestResponse<>(activityService.get(activityId, appId));
  }

  /**
   * List logs.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @param unitName   the unit name
   * @param request    the request
   * @return the rest response
   */
  @GET
  @Path("{activityId}/logs")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Log>> listLogs(@QueryParam("appId") String appId,
      @PathParam("activityId") String activityId, @QueryParam("unitName") String unitName,
      @BeanParam PageRequest<Log> request) {
    request.addFilter("activityId", EQ, activityId);
    request.addFilter("commandUnitName", EQ, unitName);
    if (dataStoreService instanceof GoogleDataStoreServiceImpl) {
      if (request.getUriInfo() != null && request.getUriInfo().getQueryParameters() != null) {
        List<SortOrder> orders = populateSortOrder(request.getUriInfo().getQueryParameters());
        if (isNotEmpty(request.getOrders())) {
          orders.addAll(request.getOrders());
        }
        request.setOrders(orders);
      }
    }
    return new RestResponse<>(logService.list(appId, request));
  }

  /**
   * List logs rest response.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @return the rest response
   */
  @GET
  @Path("{activityId}/units")
  @Timed
  public RestResponse<List<CommandUnitDetails>> getActivityCommandUnits(
      @QueryParam("appId") String appId, @PathParam("activityId") String activityId) {
    return new RestResponse<>(activityService.getCommandUnits(appId, activityId));
  }

  /**
   * Export logs response.
   *
   * @param appId      the app id
   * @param activityId the activity id
   * @return the response
   */
  @GET
  @Path("{activityId}/all-logs")
  @Encoded
  @Timed
  @ExceptionMetered
  public Response exportLogs(@QueryParam("appId") String appId, @PathParam("activityId") String activityId) {
    File logFile = logService.exportLogs(appId, activityId);
    ResponseBuilder response = Response.ok(logFile, "application/x-unknown");
    response.header("Content-Disposition", "attachment; filename=" + logFile.getName());
    return response.build();
  }

  @GET
  @Path("{stateExecutionId}/api-call-logs")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<ThirdPartyApiCallLog>> listLogs(@QueryParam("appId") String appId,
      @PathParam("stateExecutionId") String stateExecutionId, @QueryParam("startTime") long startTime,
      @QueryParam("endTime") long endTime, @BeanParam PageRequest<ThirdPartyApiCallLog> request) {
    Preconditions.checkState(endTime >= startTime, "End time should be greater than start time");

    request.addFilter(ThirdPartyApiCallLogKeys.stateExecutionId, EQ, stateExecutionId);
    if (startTime > 0) {
      request.addFilter(CreatedAtAware.CREATED_AT_KEY, GE, startTime);
    }

    if (endTime > 0) {
      request.addFilter(CreatedAtAware.CREATED_AT_KEY, LT_EQ, endTime);
    }

    if (isEmpty(request.getOrders())) {
      request.setOrders(asList(aSortOrder().withField(ThirdPartyApiCallLog.CREATED_AT_KEY, DESC).build()));
    }
    PageResponse<ThirdPartyApiCallLog> response =
        wingsPersistence.query(ThirdPartyApiCallLog.class, request, excludeAuthority);

    if (response.isEmpty() && dataStoreService instanceof GoogleDataStoreServiceImpl) {
      response = dataStoreService.list(ThirdPartyApiCallLog.class, request);
    }
    return new RestResponse<>(response);
  }

  private static List<SortOrder> populateSortOrder(MultivaluedMap<String, String> map) {
    List<SortOrder> sortOrders = new ArrayList<>();
    Pattern sortField = Pattern.compile("sort\\[[0-9]+]\\[field]");
    int fieldCount = 0;
    for (Map.Entry<String, List<String>> entry : map.entrySet()) {
      String key = entry.getKey();
      if (sortField.matcher(key).matches()) {
        fieldCount++;
      }
    }
    for (int index = 0; index < fieldCount; index++) {
      String key = "sort[" + index + "]";
      final String name = map.getFirst(key + "[field]");
      final String direction = map.getFirst(key + "[direction]");
      if (StringUtils.isNotBlank(direction)) {
        SortOrder sortOrder =
            SortOrder.Builder.aSortOrder().withField(name, SortOrder.OrderType.valueOf(direction)).build();
        sortOrders.add(sortOrder);
      }
    }
    return sortOrders;
  }
}
