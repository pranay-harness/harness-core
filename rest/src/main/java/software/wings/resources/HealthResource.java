package software.wings.resources;

import static software.wings.beans.ResponseMessage.Acuteness.HARMLESS;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.RestResponse;
import software.wings.exception.WingsException;
import software.wings.security.annotations.PublicApi;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by brett on 12/3/17
 */
@Api("health")
@Path("/health")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PublicApi
public class HealthResource {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<String> get() {
    if (isMaintenance()) {
      logger.info("In maintenance mode. Throwing exception for letting load balancer know.");
      throw new WingsException(aResponseMessage().code(ErrorCode.RESOURCE_NOT_FOUND).acuteness(HARMLESS).build());
    }
    return new RestResponse<>("healthy");
  }
}
