package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.dynatrace.DynaTraceApplication;
import software.wings.service.impl.dynatrace.DynaTraceSetupTestNodeData;
import software.wings.service.intfc.dynatrace.DynaTraceService;

import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * DynaTrace Resource containing REST endpoints
 * Created by Pranjal on 09/12/2018
 */
@Api("dynatrace")
@Path("/dynatrace")
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class DynaTraceResource {
  @Inject private DynaTraceService dynatraceService;
  @POST
  @Path("/node-data")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      @QueryParam("accountId") final String accountId, @Valid DynaTraceSetupTestNodeData setupTestNodeData) {
    return new RestResponse<>(dynatraceService.getMetricsWithDataForNode(setupTestNodeData));
  }

  @GET
  @Path("/services")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<List<DynaTraceApplication>> getDynatraceServices(@QueryParam("settingId") String settingId) {
    return new RestResponse<>(dynatraceService.getServices(settingId));
  }
}
