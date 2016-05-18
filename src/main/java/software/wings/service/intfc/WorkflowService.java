package software.wings.service.intfc;

import software.wings.beans.Orchestration;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.sm.StateMachine;
import software.wings.sm.StateTypeDescriptor;

import java.util.List;

import javax.validation.Valid;

/**
 * @author Rishi
 */
public interface WorkflowService {
  <T extends Workflow> T createWorkflow(Class<T> cls, T workflow);

  <T extends Workflow> T updateWorkflow(Class<T> cls, T workflow);

  PageResponse<Pipeline> listPipelines(PageRequest<Pipeline> req);

  Pipeline readPipeline(String appId, String pipelineId);

  StateMachine create(@Valid StateMachine stateMachine);

  StateMachine read(String smId);

  StateMachine readLatest(String originId, String name);

  PageResponse<StateMachine> list(PageRequest<StateMachine> req);

  void trigger(String smId);

  List<StateTypeDescriptor> stencils();

  PageResponse<Orchestration> listOrchestration(PageRequest<Orchestration> pageRequest);

  Orchestration readOrchestration(String appId, String envId, String orchestrationId);

  PageResponse<WorkflowExecution> listExecutions(PageRequest<WorkflowExecution> pageRequest, boolean includeGraph);

  WorkflowExecution triggerPipelineExecution(String appId, String pipelineId);

  WorkflowExecution triggerOrchestrationExecution(String appId, String orchestrationId, List<String> artifactIds);

  WorkflowExecution getExecutionDetails(String appId, String workflowExecutionId);
}
