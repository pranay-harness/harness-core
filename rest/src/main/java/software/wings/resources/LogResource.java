package software.wings.resources;

import io.swagger.annotations.Api;
import software.wings.beans.Log;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.DelegateAuth;
import software.wings.service.intfc.LogService;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Created by peeyushaggarwal on 1/9/17.
 */
@Api("logs")
@Path("/logs")
@Produces("application/json")
public class LogResource {
  private LogService logService;

  @Inject
  public LogResource(LogService logService) {
    this.logService = logService;
  }

  @DelegateAuth
  @POST
  public RestResponse<Log> save(Log log) {
    return new RestResponse<>(logService.save(log));
  }
}
