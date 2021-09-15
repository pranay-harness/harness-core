/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.core.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.validation.Validator.notNullCheck;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.service.GitSyncTriggerService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.PublicApiWithWhitelist;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import lombok.AllArgsConstructor;

@Api("/git-sync-trigger")
@Path("/git-sync-trigger")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitSyncTriggerResource {
  GitSyncTriggerService gitSyncTriggerService;

  @POST
  @Path("webhook/{entityToken}")
  @Consumes(APPLICATION_JSON)
  @Timed
  @ExceptionMetered
  @PublicApiWithWhitelist
  public RestResponse webhookCatcher(@QueryParam("accountId") String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId, @PathParam("entityToken") String entityToken,
      String yamlWebHookPayload, @Context HttpHeaders httpHeaders) {
    notNullCheck("webhook token", entityToken);
    return new RestResponse<>(gitSyncTriggerService.validateAndQueueWebhookRequest(
        accountId, orgId, projectId, entityToken, yamlWebHookPayload, httpHeaders));
  }
}
