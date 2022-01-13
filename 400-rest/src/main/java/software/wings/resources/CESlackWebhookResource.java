/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
