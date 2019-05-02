package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.harness.version.RuntimeInfo;
import io.harness.version.VersionInfoManager;
import io.harness.version.VersionPackage;
import io.swagger.annotations.Api;
import software.wings.app.DeployMode;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.security.annotations.PublicApi;

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
  public RestResponse<VersionPackage> get() {
    final String deployMode = System.getenv().getOrDefault(DeployMode.DEPLOY_MODE, "KUBERNETES");
    DeployMode deployMode1 = DeployMode.valueOf(deployMode);
    return new RestResponse<>(VersionPackage.builder()
                                  .versionInfo(versionInfoManager.getVersionInfo())
                                  .runtimeInfo(RuntimeInfo.builder()
                                                   .primary(configurationController.isPrimary())
                                                   .primaryVersion(configurationController.getPrimaryVersion())
                                                   .deployMode(deployMode1.getDeployedAs())
                                                   .build())
                                  .build());
  }
}
