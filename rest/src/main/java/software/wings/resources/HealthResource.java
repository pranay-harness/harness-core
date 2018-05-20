package software.wings.resources;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorCode.RESOURCE_NOT_FOUND;
import static software.wings.core.maintenance.MaintenanceController.forceMaintenance;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;
import static software.wings.core.maintenance.MaintenanceController.unforceMaintenance;
import static software.wings.exception.WingsException.USER;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.RestResponse;
import software.wings.exception.WingsException;
import software.wings.security.annotations.PublicApi;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
  @Inject MainConfiguration mainConfiguration;

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

  @GET
  @Path("maintenance")
  @Timed
  @ExceptionMetered
  public RestResponse<String> setMaintenance(@QueryParam("key") String key, @QueryParam("value") String value) {
    if (isNotBlank(key) && key.equals(mainConfiguration.getMaintenanceKey())) {
      if ("true".equals(value)) {
        forceMaintenance(true);
        return new RestResponse<>("maintenance true");
      } else if ("false".equals(value)) {
        forceMaintenance(false);
        return new RestResponse<>("maintenance false");
      } else if ("none".equals(value)) {
        unforceMaintenance();
        return new RestResponse<>("maintenance none");
      } else {
        throw new WingsException(INVALID_ARGUMENT, USER).addParam("value", value);
      }
    }
    throw new WingsException(INVALID_CREDENTIAL, USER);
  }
}
