package io.harness.ng.orchestration;

import io.harness.dto.OrchestrationGraphDTO;
import io.harness.engine.OrchestrationService;
import io.harness.execution.PlanExecution;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.redesign.services.CustomExecutionProvider;
import io.harness.redesign.services.CustomExecutionService;
import io.harness.rest.RestResponse;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.StreamingOutput;

@Path("/orchestration")
@Api("/orchestration")
@Produces("application/json")
@Consumes("application/json")
public class NgOrchestrationResource {
  @Inject private OrchestrationService orchestrationService;
  @Inject private CustomExecutionService customExecutionService;
  @Inject private CustomExecutionProvider customExecutionProvider;

  @GET
  @Path("/http-v2")
  @ApiOperation(value = "Triggers a task v2 Plan", nickname = "http-v2")
  public RestResponse<PlanExecution> triggerHttpV2Plan(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("appId") @NotNull String appId) {
    PlanExecution execution = orchestrationService.startExecution(
        customExecutionProvider.provideHttpSwitchPlanV2(), getAbstractions(accountId, appId));
    return new RestResponse<>(execution);
  }

  @GET
  @Path("/http-v3")
  @ApiOperation(value = "Triggers a task v3 Plan", nickname = "http-v3")
  public RestResponse<PlanExecution> triggerHttpV3Plan(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("appId") @NotNull String appId) {
    PlanExecution execution = orchestrationService.startExecution(
        customExecutionProvider.provideHttpSwitchPlanV3(), getAbstractions(accountId, appId));
    return new RestResponse<>(execution);
  }

  @GET
  @Path("/http-chain-v2")
  @ApiOperation(value = "Triggers a task chain v2 Plan", nickname = "http-chain-v2")
  public RestResponse<PlanExecution> triggerHttpChainV2Plan(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("appId") @NotNull String appId) {
    PlanExecution execution = orchestrationService.startExecution(
        customExecutionProvider.provideTaskChainPlan(OrchestrationFacilitatorType.TASK_CHAIN),
        getAbstractions(accountId, appId));
    return new RestResponse<>(execution);
  }

  @GET
  @Path("/http-chain-v3")
  @ApiOperation(value = "Triggers a task chain v3 Plan", nickname = "http-chain-v3")
  public RestResponse<PlanExecution> triggerHttpChainV3Plan(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("appId") @NotNull String appId) {
    PlanExecution execution = orchestrationService.startExecution(
        customExecutionProvider.provideTaskChainPlan(OrchestrationFacilitatorType.TASK_CHAIN),
        getAbstractions(accountId, appId));
    return new RestResponse<>(execution);
  }

  @GET
  @Path("/get-graph")
  @ApiOperation(value = "generate graph for plan execution", nickname = "get-graph")
  public RestResponse<OrchestrationGraphDTO> getGraph(@QueryParam("planExecutionId") String planExecutionId) {
    return new RestResponse<>(customExecutionService.getOrchestrationGraphV2(planExecutionId));
  }

  @GET
  @Path("/get-graph-visualization")
  @Produces("image/png")
  @ApiOperation(value = "generate graph execution visualization", nickname = "get-graph-visualization")
  public StreamingOutput getGraphVisualization(@QueryParam("planExecutionId") String planExecutionId) {
    return output -> customExecutionService.getGraphVisualization(planExecutionId, output);
  }

  @GET
  @Path("/get-graph-orchestration")
  @ApiOperation(value = "generate orchestration graph for plan execution", nickname = "get-graph-orchestration")
  public RestResponse<OrchestrationGraphDTO> getOrchestrationGraph(
      @QueryParam("planExecutionId") String planExecutionId) {
    return new RestResponse<>(customExecutionService.getOrchestrationGraphV2(planExecutionId));
  }

  private Map<String, String> getAbstractions(String accountId, String appId) {
    return ImmutableMap.of("accountId", accountId, "appId", appId, "userId", "lv0euRhKRCyiXWzS7pOg6g", "userName",
        "Admin", "userEmail", "admin@harness.io");
  }
}
