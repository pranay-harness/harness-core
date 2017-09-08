package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.ServerInfo;

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
