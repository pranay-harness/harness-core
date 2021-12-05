package io.harness.cvng.servicelevelobjective.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardApiFilter;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardWidget;
import io.harness.cvng.servicelevelobjective.services.api.SLODashboardService;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Api("slo-dashboard")
@Path("slo-dashboard")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
public class SLODashboardResource {
  @Inject private SLODashboardService sloDashboardService;
  @GET
  @Path("widgets")
  @ExceptionMetered
  @ApiOperation(value = "get widget list", nickname = "getSLODashboardWidgets")
  public ResponseDTO<PageResponse<SLODashboardWidget>> getSloDashboardWidgets(@BeanParam ProjectParams projectParams,
      @BeanParam SLODashboardApiFilter filter, @BeanParam PageParams pageParams) {
    return ResponseDTO.newResponse(sloDashboardService.getSloDashboardWidgets(projectParams, filter, pageParams));
  }
}
