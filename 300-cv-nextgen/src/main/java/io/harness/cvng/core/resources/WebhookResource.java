/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.resources;

import io.harness.cvng.core.services.api.WebhookService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("webhook")
@Path("webhook")
@Produces("application/json")
@NextGenManagerAuth
public class WebhookResource {
  @Inject WebhookService webhookService;

  @GET
  @Path("create-token")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "creates a webhook token to register activities", nickname = "createWebhookToken")
  public RestResponse<String> createToken(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("projectIdentifier") final String projectIdentifier,
      @QueryParam("orgIdentifier") final String orgIdentifier) {
    return new RestResponse<>(webhookService.createWebhookToken(projectIdentifier, orgIdentifier));
  }

  @GET
  @Path("recreate-token")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "recreates a webhook token to register activities", nickname = "recreateWebhookToken")
  public RestResponse<String> recreateToken(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("projectIdentifier") final String projectIdentifier,
      @QueryParam("orgIdentifier") final String orgIdentifier) {
    return new RestResponse<>(webhookService.recreateWebhookToken(projectIdentifier, orgIdentifier));
  }

  @DELETE
  @Path("delete-token")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "deletes a webhook token", nickname = "deleteWebhookToken")
  public void deleteToken(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("projectIdentifier") final String projectIdentifier,
      @QueryParam("orgIdentifier") final String orgIdentifier) {
    webhookService.deleteWebhookToken(projectIdentifier, orgIdentifier);
  }
}
