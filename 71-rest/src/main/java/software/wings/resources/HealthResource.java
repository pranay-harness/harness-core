package software.wings.resources;

import static io.harness.eraro.ErrorCode.RESOURCE_NOT_FOUND;
import static io.harness.exception.WingsException.USER;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.exception.WingsException;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.RestResponse;
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
  private static final Logger logger = LoggerFactory.getLogger(HealthResource.class);

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<String> get() {
    if (isMaintenance()) {
      logger.info("In maintenance mode. Throwing exception to prevent traffic.");
      throw new WingsException(RESOURCE_NOT_FOUND, USER);
    }
    return new RestResponse<>("healthy");
  }
}
