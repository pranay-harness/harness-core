/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.USER;

import io.harness.ccm.communication.CESlackWebhookService;
import io.harness.ccm.communication.entities.CESlackWebhook;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("ceSlackWebhook")
@Path("/ceSlackWebhooks")
@Produces("application/json")
@Scope(USER)
public class CESlackWebhookResource {
  private CESlackWebhookService ceSlackWebhookService;

  @Inject
  public CESlackWebhookResource(CESlackWebhookService ceSlackWebhookService) {
    this.ceSlackWebhookService = ceSlackWebhookService;
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<CESlackWebhook> get(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(ceSlackWebhookService.getByAccountId(accountId));
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<CESlackWebhook> save(@QueryParam("accountId") String accountId, CESlackWebhook slackWebhook) {
    slackWebhook.setAccountId(accountId);
    return new RestResponse<>(ceSlackWebhookService.upsert(slackWebhook));
  }
}
