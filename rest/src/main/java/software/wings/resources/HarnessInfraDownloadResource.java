package software.wings.resources;

import com.google.inject.Inject;

import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.DelegateAuth;
import software.wings.service.impl.infra.InfraDownloadService;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api("infra-download")
@Path("/infra-download")
public class HarnessInfraDownloadResource {
  @Inject InfraDownloadService infraDownloadService;

  @GET
  @Path("delegate-auth/watcher/{version}")
  @Produces(MediaType.APPLICATION_JSON)
  @DelegateAuth
  public RestResponse<String> getWatcherDownloadUrlFromDelegate(@PathParam("version") String version) {
    return new RestResponse<String>(infraDownloadService.getDownloadUrlForWatcher(version));
  }

  @GET
  @Path("delegate-auth/delegate/{version}")
  @Produces(MediaType.APPLICATION_JSON)
  @DelegateAuth
  public RestResponse<String> getDelegateDownloadUrlFromDelegate(@PathParam("version") String version) {
    return new RestResponse<String>(infraDownloadService.getDownloadUrlForDelegate(version));
  }

  @GET
  @Path("default/watcher/{version}")
  @Produces(MediaType.APPLICATION_JSON)
  public RestResponse<String> getWatcherDownloadUrlFromDefaultAuth(@PathParam("version") String version,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("env") @DefaultValue("") String env) {
    return new RestResponse<String>(infraDownloadService.getDownloadUrlForWatcher(version, env));
  }

  @GET
  @Path("default/delegate/{version}")
  @Produces(MediaType.APPLICATION_JSON)
  public RestResponse<String> getDelegateDownloadUrlFromDefaultAuth(@PathParam("version") String version,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("env") @DefaultValue("") String env) {
    return new RestResponse<String>(infraDownloadService.getDownloadUrlForDelegate(version, env));
  }
}
