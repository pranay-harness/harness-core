/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resource;

import io.harness.controller.PrimaryVersionController;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.PublicApi;
import io.harness.version.RuntimeInfo;
import io.harness.version.VersionInfoManager;
import io.harness.version.VersionPackage;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api("version")
@Path("/version")
@Produces(MediaType.APPLICATION_JSON)
@PublicApi
public class VersionInfoResource {
  @Inject private VersionInfoManager versionInfoManager;
  @Inject private PrimaryVersionController primaryVersionController;

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get version for CVNG service", nickname = "getCvVersion")
  public RestResponse<VersionPackage> get() {
    return new RestResponse<>(VersionPackage.builder()
                                  .versionInfo(versionInfoManager.getVersionInfo())
                                  .runtimeInfo(RuntimeInfo.builder()
                                                   .primaryVersion(primaryVersionController.getPrimaryVersion())
                                                   .primary(primaryVersionController.isPrimary())
                                                   .build())
                                  .build());
  }
}
