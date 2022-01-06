/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.rest.RestResponse;
import io.harness.security.annotations.PublicApi;

import software.wings.service.impl.slack.SlackApprovalUtils;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Api("slack")
@Path("/slack")
@Produces(MediaType.APPLICATION_JSON)
public class SlackApprovalResource {
  @Inject private SlackApprovalUtils slackApprovalUtils;

  @POST
  @Path("approval")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @PublicApi
  public RestResponse<Boolean> handleSlackApproval(MultivaluedMap<String, String> body,
      @Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {
    return slackApprovalUtils.slackApprovalHandler(body);
  }
}
