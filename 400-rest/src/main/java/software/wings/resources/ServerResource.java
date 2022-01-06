/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.rest.RestResponse;

import software.wings.beans.ServerInfo;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import java.time.ZoneId;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Created by anubhaw on 10/19/16.
 */

@Api("/server")
@Path("/server")
@Produces("application/json")
public class ServerResource {
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<ServerInfo> getServerInfo() {
    ServerInfo serverInfo = new ServerInfo();
    serverInfo.setZoneId(ZoneId.of("America/Los_Angeles"));
    return new RestResponse<>(serverInfo);
  }
}
