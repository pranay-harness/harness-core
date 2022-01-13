/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATIONS;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CgEventConfig;
import io.harness.rest.RestResponse;
import io.harness.service.EventConfigService;
import io.harness.service.EventService;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Api("events-config")
@Path("/events-config")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Scope(ResourceType.APPLICATION)
public class CgEventsConfigResource {
  @Inject private EventConfigService eventConfigService;
  @Inject private EventService eventService;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<CgEventConfig>> list(
      @QueryParam("appId") String appId, @QueryParam("accountId") String accountId) {
    return new RestResponse<>(eventConfigService.listAllEventsConfig(accountId, appId));
  }

  @GET
  @Path("{eventConfigId}")
  @Timed
  @ExceptionMetered
  public RestResponse<CgEventConfig> getEventConfig(@PathParam("eventConfigId") String eventConfigId,
      @QueryParam("appId") String appId, @QueryParam("accountId") String accountId) {
    return new RestResponse<>(eventConfigService.getEventsConfig(accountId, appId, eventConfigId));
  }

  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_APPLICATIONS)
  public RestResponse<CgEventConfig> createEventConfig(@QueryParam("appId") String appId,
      @QueryParam("accountId") String accountId, @Valid @NotNull CgEventConfig eventConfig) {
    eventConfig.setAccountId(accountId);
    eventConfig.setAppId(appId);
    return new RestResponse<>(eventConfigService.createEventsConfig(accountId, appId, eventConfig));
  }

  @PUT
  @Path("{eventConfigId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_APPLICATIONS)
  public RestResponse<CgEventConfig> updateEventConfig(@PathParam("eventConfigId") String eventConfigId,
      @QueryParam("appId") String appId, @QueryParam("accountId") String accountId,
      @Valid @NotNull CgEventConfig eventConfig) {
    eventConfig.setAccountId(accountId);
    eventConfig.setAppId(appId);
    eventConfig.setUuid(eventConfigId);
    return new RestResponse<>(eventConfigService.updateEventsConfig(accountId, appId, eventConfig));
  }

  @PUT
  @Path("{eventConfigId}/enable")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_APPLICATIONS)
  public RestResponse<CgEventConfig> updateToggle(@PathParam("eventConfigId") String eventConfigId,
      @QueryParam("appId") String appId, @QueryParam("accountId") String accountId, CgEventConfig eventConfig) {
    eventConfig.setAccountId(accountId);
    eventConfig.setAppId(appId);
    eventConfig.setUuid(eventConfigId);
    return new RestResponse<>(eventConfigService.updateEventsConfigEnable(accountId, appId, eventConfig));
  }

  @DELETE
  @Path("{eventConfigId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_APPLICATIONS)
  public RestResponse<CgEventConfig> deleteEventConfig(@PathParam("eventConfigId") String eventConfigId,
      @QueryParam("appId") String appId, @QueryParam("accountId") String accountId) {
    eventConfigService.deleteEventsConfig(accountId, appId, eventConfigId);
    return new RestResponse<>();
  }

  @POST
  @Path("{eventConfigId}/test")
  @Timed
  @ExceptionMetered
  public RestResponse<Void> sendTestEvent(@PathParam("eventConfigId") String eventConfigId,
      @QueryParam("appId") String appId, @QueryParam("accountId") String accountId) {
    eventService.sendTestEvent(accountId, appId, eventConfigId);
    return new RestResponse<>();
  }
}
