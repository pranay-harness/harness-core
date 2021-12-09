package io.harness.cvng.core.resources;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.TimeGraphResponse;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorDTO;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelIndicatorService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.sun.istack.internal.NotNull;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import retrofit2.http.Body;

@Api("monitored-service/sli")
@Path("monitored-service/{monitoredServiceIdentifier}/sli")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
@OwnedBy(HarnessTeam.CV)
public class ServiceLevelIndicatorResource {
  @Inject ServiceLevelIndicatorService sliService;

  @POST
  @Timed
  @ExceptionMetered
  @Path("/onboarding-graph")
  @ApiOperation(value = "get Sli graph for onboarding UI", nickname = "getSliGraph")
  public RestResponse<TimeGraphResponse> getGraph(@BeanParam ProjectParams projectParams,
      @PathParam("monitoredServiceIdentifier") String monitoredServiceIdentifier,
      @NotNull @Valid @Body ServiceLevelIndicatorDTO serviceLevelIndicatorDTO) {
    return new RestResponse<>(sliService.getOnboardingGraph(
        projectParams, monitoredServiceIdentifier, serviceLevelIndicatorDTO, generateUuid()));
  }
}
