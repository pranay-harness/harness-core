package software.wings.graphql.datafetcher.execution;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.graphql.datafetcher.service.ServiceController;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;
import software.wings.graphql.schema.query.QLExecutionInputsToResumePipelineQueryParams;
import software.wings.graphql.schema.type.QLService;
import software.wings.graphql.schema.type.execution.QLExecutionInputs;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
@Singleton
public class RuntimeInputExecutionInputsController {
  @Inject ServiceResourceService serviceResourceService;
  @Inject PipelineExecutionController pipelineExecutionController;
  @Inject PipelineService pipelineService;
  @Inject AuthHandler authHandler;
  @Inject WorkflowExecutionService workflowExecutionService;

  public QLExecutionInputs fetch(QLExecutionInputsToResumePipelineQueryParams parameters, String accountId) {
    try (AutoLogContext ignore0 = new AccountLogContext(accountId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      // TODO: Test cases
      String appId = parameters.getApplicationId();

      WorkflowExecution pipelineExecution =
          workflowExecutionService.getWorkflowExecution(appId, parameters.getPipelineExecutionId());
      String pipelineId = pipelineExecution.getWorkflowId();
      Pipeline pipeline = pipelineService.readPipeline(appId, pipelineId, true);
      pipelineExecutionController.handleAuthentication(appId, pipeline);
      // Validate Required changes
      List<String> serviceIds = getArtifactNeededServices(appId, parameters, pipeline);
      if (isEmpty(serviceIds)) {
        return QLExecutionInputs.builder().serviceInputs(new ArrayList<>()).build();
      }
      PageRequest<Service> pageRequest = aPageRequest()
                                             .addFilter(ServiceKeys.appId, EQ, parameters.getApplicationId())
                                             .addFilter(ServiceKeys.accountId, EQ, accountId)
                                             .addFilter("_id", IN, serviceIds.toArray())
                                             .build();
      List<QLService> qlServices = serviceResourceService.list(pageRequest, false, false, false, null)
                                       .stream()
                                       .map(ServiceController::buildQLService)
                                       .collect(Collectors.toList());

      return QLExecutionInputs.builder().serviceInputs(qlServices).build();
    }
  }

  private List<String> getArtifactNeededServices(
      String appId, QLExecutionInputsToResumePipelineQueryParams params, Pipeline pipeline) {
    String pipelineId = pipeline.getUuid();
    List<QLVariableInput> variableInputs = params.getVariableInputs();
    if (variableInputs == null) {
      variableInputs = new ArrayList<>();
    }
    String envId = pipelineExecutionController.resolveEnvId(pipeline, variableInputs);
    List<String> extraVariables = new ArrayList<>();

    Map<String, String> variableValues = pipelineExecutionController.validateAndResolvePipelineVariables(
        pipeline, variableInputs, envId, extraVariables, false);
    Map<String, String> workflowVariables = new HashMap<>();
    DeploymentMetadata finalDeploymentMetadata = workflowExecutionService.fetchDeploymentMetadataRunningPipeline(
        appId, workflowVariables, false, params.getPipelineExecutionId(), params.getPipelineStageElementId());
    if (finalDeploymentMetadata != null) {
      List<String> artifactNeededServiceIds = finalDeploymentMetadata.getArtifactRequiredServiceIds();
      if (isNotEmpty(artifactNeededServiceIds)) {
        return artifactNeededServiceIds;
      }
    }
    log.info("No Services requires artifact inputs for this pipeline: " + pipelineId);
    return new ArrayList<>();
  }
}