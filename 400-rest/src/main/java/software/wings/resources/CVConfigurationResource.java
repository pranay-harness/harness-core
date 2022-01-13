/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.rest.RestResponse;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

/**
 * @author Vaibhav Tulsyan
 * 08/Oct/2018
 */

@Api("cv-configuration")
@Path("/cv-configuration")
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class CVConfigurationResource {
  @Inject CVConfigurationService cvConfigurationService;
  @Inject EventPublishHelper eventPublishHelper;

  @GET
  @Path("{serviceConfigurationId}")
  @Timed
  @ExceptionMetered
  public <T extends CVConfiguration> RestResponse<T> getConfiguration(
      @QueryParam("accountId") @Valid final String accountId,
      @PathParam("serviceConfigurationId") String serviceConfigurationId) {
    return new RestResponse<>(cvConfigurationService.getConfiguration(serviceConfigurationId));
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveCVConfiguration(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("appId") @Valid final String appId, @QueryParam("stateType") StateType stateType,
      @Body Object params) {
    String cvConfigId = cvConfigurationService.saveConfiguration(accountId, appId, stateType, params);
    eventPublishHelper.publishSetupCV247Event(accountId, cvConfigId);
    return new RestResponse<>(cvConfigId);
  }

  @GET
  @Timed
  @ExceptionMetered
  public <T extends CVConfiguration> RestResponse<List<T>> listConfigurations(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("appId") @Valid final String appId,
      @QueryParam("envId") final String envId, @QueryParam("stateType") final StateType stateType) {
    return new RestResponse<>(cvConfigurationService.listConfigurations(accountId, appId, envId, stateType));
  }

  @GET
  @Path("/list-cv-configurations")
  @Timed
  @ExceptionMetered
  public RestResponse<List<CVConfiguration>> listConfigurations(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("appIds") final List<String> appIds, @QueryParam("envIds") final List<String> envIds) {
    return new RestResponse<>(cvConfigurationService.listConfigurations(accountId, appIds, envIds));
  }

  @PUT
  @Path("{serviceConfigurationId}")
  @Timed
  @ExceptionMetered
  public RestResponse<String> updateCVConfiguration(@PathParam("serviceConfigurationId") String serviceConfigurationId,
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("appId") @Valid final String appId,
      @QueryParam("stateType") StateType stateType, @Body Object params) {
    return new RestResponse<>(
        cvConfigurationService.updateConfiguration(accountId, appId, stateType, params, serviceConfigurationId));
  }

  @DELETE
  @Path("{serviceConfigurationId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteCVConfiguration(@PathParam("serviceConfigurationId") String serviceConfigurationId,
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("appId") @Valid final String appId) {
    return new RestResponse<>(cvConfigurationService.deleteConfiguration(accountId, appId, serviceConfigurationId));
  }

  @POST
  @Path("/reset-baseline")
  @Timed
  @ExceptionMetered
  public RestResponse<String> resetBaseline(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("appId") @Valid final String appId, final @Valid @QueryParam("cvConfigId") String cvConfigId,
      @Body LogsCVConfiguration logsCVConfiguration) {
    return new RestResponse<>(cvConfigurationService.resetBaseline(appId, cvConfigId, logsCVConfiguration));
  }

  @POST
  @Path("/update-alert-setting")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> updateAlertSettings(@QueryParam("accountId") @Valid final String accountId,
      final @Valid @QueryParam("cvConfigId") String cvConfigId, @Body CVConfiguration cvConfiguration) {
    return new RestResponse<>(cvConfigurationService.updateAlertSettings(cvConfigId, cvConfiguration));
  }

  @POST
  @Path("/update-snooze")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> updateSnooze(@QueryParam("accountId") @Valid final String accountId,
      final @Valid @QueryParam("cvConfigId") String cvConfigId, @Body CVConfiguration cvConfiguration) {
    return new RestResponse<>(cvConfigurationService.updateSnooze(cvConfigId, cvConfiguration));
  }
}
