package io.harness.ng;

import io.harness.configManager.ConfigurationController;
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
  @Inject private ConfigurationController configurationController;

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get Version Info", nickname = "getVersionInfo")
  public RestResponse<VersionPackage> get() {
    return new RestResponse<>(VersionPackage.builder()
                                  .versionInfo(versionInfoManager.getVersionInfo())
                                  .runtimeInfo(RuntimeInfo.builder()
                                                   .primaryVersion(configurationController.getPrimaryVersion())
                                                   .primary(configurationController.isPrimary())
                                                   .build())
                                  .build());
  }
}
