package io.harness.delegate.app.resource;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;
import static io.harness.maintenance.MaintenanceController.getMaintenanceFlag;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.NoResultFoundException;
import io.harness.health.HealthException;
import io.harness.health.HealthService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.PublicApi;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

@Api("health")
@Path("/health")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@OwnedBy(DEL)
@Slf4j
@PublicApi
public class HealthResource {
  private final HealthService healthService;

  @Inject
  public HealthResource(HealthService healthService) {
    this.healthService = healthService;
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get health for delegate agent", nickname = "getDelegateAgentHealthStatus")
  public ResponseDTO<String> get() throws Exception {
    final HealthCheck.Result check = healthService.check();
    if (check.isHealthy()) {
      return ResponseDTO.newResponse("healthy");
    }
    throw new HealthException(check.getMessage(), check.getError());
  }
}