package io.harness.ng.orchestration;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.engine.OrchestrationService;
import io.harness.execution.PlanExecution;
import io.harness.redesign.services.CustomExecutionUtils;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/orchestration")
@Api("/orchestration")
@Produces("application/json")
@Consumes("application/json")
public class NgOrchestrationResource {
  @Inject private OrchestrationService orchestrationService;

  private static final EmbeddedUser EMBEDDED_USER =
      EmbeddedUser.builder().uuid("lv0euRhKRCyiXWzS7pOg6g").email("admin@harness.io").name("Admin").build();

  @GET
  @Path("/http-v2")
  @ApiOperation(value = "Triggers a task v2 Plan", nickname = "http-v2")
  public RestResponse<PlanExecution> triggerHttpV2Plan(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("appId") @NotNull String appId) {
    PlanExecution execution = orchestrationService.startExecution(
        CustomExecutionUtils.provideHttpSwitchPlanV2(), getAbstractions(accountId, appId), EMBEDDED_USER);
    return new RestResponse<>(execution);
  }

  private Map<String, String> getAbstractions(String accountId, String appId) {
    return ImmutableMap.of("accountId", accountId, "appId", appId);
  }
}
