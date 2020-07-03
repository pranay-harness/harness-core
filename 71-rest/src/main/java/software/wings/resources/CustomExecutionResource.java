package software.wings.resources;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.execution.PlanExecution;
import io.harness.interrupts.Interrupt;
import io.harness.presentation.Graph;
import io.harness.redesign.services.CustomExecutionService;
import io.harness.rest.RestResponse;

import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.StreamingOutput;

@Redesign
@Path("/execute2")
@Produces("application/json")
public class CustomExecutionResource {
  @Inject CustomExecutionService customExecutionService;

  @GET
  @Path("/http-switch")
  public RestResponse<PlanExecution> executeHttpSwitch() {
    return new RestResponse<>(customExecutionService.executeHttpSwitch());
  }

  @GET
  @Path("/http-fork")
  public RestResponse<PlanExecution> executeHttpFork() {
    return new RestResponse<>(customExecutionService.executeHttpFork());
  }

  @GET
  @Path("/http-section")
  public RestResponse<PlanExecution> executeSectionPlan() {
    return new RestResponse<>(customExecutionService.executeSectionPlan());
  }

  @GET
  @Path("/http-retry-ignore")
  public RestResponse<PlanExecution> executeRetryIgnorePlan() {
    return new RestResponse<>(customExecutionService.executeRetryIgnorePlan());
  }

  @GET
  @Path("/http-retry-abort")
  public RestResponse<PlanExecution> executeRetryAbortPlan() {
    return new RestResponse<>(customExecutionService.executeRetryAbortPlan());
  }

  @GET
  @Path("/http-rollback")
  public RestResponse<PlanExecution> executeRollbackPlan() {
    return new RestResponse<>(customExecutionService.executeRollbackPlan());
  }

  @GET
  @Path("/simple-shell-script")
  public RestResponse<PlanExecution> executeSimpleShellScriptPlan(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId) {
    return new RestResponse<>(customExecutionService.executeSimpleShellScriptPlan(accountId, appId));
  }

  @GET
  @Path("/task-chain")
  public RestResponse<PlanExecution> executeTaskChainPlan() {
    return new RestResponse<>(customExecutionService.executeTaskChainPlan());
  }

  @GET
  @Path("/section-chain")
  public RestResponse<PlanExecution> executeSectionChainPlan() {
    return new RestResponse<>(customExecutionService.executeSectionChainPlan());
  }

  @GET
  @Path("/section-chain-rollback")
  public RestResponse<PlanExecution> executeSectionChainRollbackPlan() {
    return new RestResponse<>(customExecutionService.executeSectionChainRollbackPlan());
  }

  @GET
  @Path("/abort-plan")
  public RestResponse<Interrupt> abortPlan(
      @QueryParam("accountId") String accountId, @QueryParam("planExecutionId") String planExecutionId) {
    return new RestResponse<>(customExecutionService.registerInterrupt(planExecutionId));
  }

  @GET
  @Path("/test-infra-state")
  public RestResponse<PlanExecution> testInfraState() throws IOException {
    return new RestResponse<>(customExecutionService.testInfraState());
  }

  @GET
  @Path("/test-graph-plan")
  public RestResponse<PlanExecution> testGraphPlan() {
    return new RestResponse<>(customExecutionService.testGraphPlan());
  }

  @GET
  @Path("/get-graph")
  public RestResponse<Graph> getGraph(@QueryParam("planExecutionId") String planExecutionId) {
    return new RestResponse<>(customExecutionService.getGraph(planExecutionId));
  }

  @GET
  @Path("/get-graph-visualization")
  @Produces("image/png")
  public StreamingOutput getGraphVisualization(@QueryParam("planExecutionId") String planExecutionId) {
    return output -> customExecutionService.getGraphVisualization(planExecutionId, output);
  }

  @POST
  @Path("/test-execution-plan")
  @Produces("application/json")
  public RestResponse<PlanExecution> testExecutionPlan(
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, String pipelineYaml) {
    return new RestResponse<>(customExecutionService.testExecutionPlanCreator(pipelineYaml, accountId, appId));
  }

  @GET
  @Path("/test-artifact-state")
  public RestResponse<PlanExecution> testArtifactStep() {
    return new RestResponse<>(customExecutionService.testArtifactState());
  }

  @GET
  @Path("/test-service-state")
  public RestResponse<PlanExecution> testServiceStep() {
    return new RestResponse<>(customExecutionService.testServiceState());
  }

  @GET
  @Path("/single-barrier")
  public RestResponse<PlanExecution> executeSingleBarrierPlan() {
    return new RestResponse<>(customExecutionService.executeSingleBarrierPlan());
  }
}
