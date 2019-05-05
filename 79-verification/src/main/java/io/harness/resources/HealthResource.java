package io.harness.resources;

import static io.harness.eraro.ErrorCode.RESOURCE_NOT_FOUND;
import static io.harness.exception.WingsException.USER;
import static io.harness.maintenance.MaintenanceController.isMaintenance;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.health.HealthCheck;
import io.harness.exception.WingsException;
import io.harness.health.HealthException;
import io.harness.health.HealthService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.security.annotations.PublicApi;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by Praveen
 */

@Api("health")
@Path("/health")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PublicApi
@Slf4j
public class HealthResource {
  private HealthService healthService;

  @Inject
  public HealthResource(HealthService healthService) {
    this.healthService = healthService;
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<String> get() throws Exception {
    if (isMaintenance()) {
      logger.info("In maintenance mode. Throwing exception to prevent traffic.");
      throw new WingsException(RESOURCE_NOT_FOUND, USER);
    }

    final HealthCheck.Result check = healthService.check();
    if (check.isHealthy()) {
      return new RestResponse<>("healthy");
    }

    throw new HealthException(check.getMessage(), check.getError());
  }
}
