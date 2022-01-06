/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.cvng;

import static io.harness.cvng.core.services.CVNextGenConstants.CD_CURRENT_GEN_CHANGE_EVENTS_PATH;

import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.change.HarnessCDCurrentGenEventMetadata;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;

import software.wings.service.intfc.cvng.CDChangeSourceIntegrationService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.time.Instant;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

@Api(CD_CURRENT_GEN_CHANGE_EVENTS_PATH)
@Path(CD_CURRENT_GEN_CHANGE_EVENTS_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
@OwnedBy(HarnessTeam.CV)
@LearningEngineAuth
@ExposeInternalException(withStackTrace = true)
public class CDChangeSourceResource {
  @Inject private CDChangeSourceIntegrationService cdChangeSourceIntegrationService;

  @GET
  public RestResponse<List<HarnessCDCurrentGenEventMetadata>> getChangeEvents(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("serviceId") String serviceId,
      @QueryParam("environmentId") String environmentId, @QueryParam("startTime") Long startTime,
      @QueryParam("endTime") Long endTime) {
    return new RestResponse<>(cdChangeSourceIntegrationService.getCurrentGenEventsBetween(
        accountId, appId, serviceId, environmentId, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime)));
  }
}
