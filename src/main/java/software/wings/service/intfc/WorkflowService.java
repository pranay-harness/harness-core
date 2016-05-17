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
  public <T extends Workflow> T createWorkflow(Class<T> cls, T workflow);

  public <T extends Workflow> T updateWorkflow(Class<T> cls, T workflow);

  public PageResponse<Pipeline> listPipelines(PageRequest<Pipeline> req);

  public Pipeline readPipeline(String appId, String pipelineId);

  public StateMachine create(@Valid StateMachine stateMachine);

  public StateMachine read(String smId);

  public StateMachine readLatest(String originId, String name);

  public PageResponse<StateMachine> list(PageRequest<StateMachine> req);

  public void trigger(String smId);

  public List<StateTypeDescriptor> stencils();

  public PageResponse<Orchestration> listOrchestration(PageRequest<Orchestration> pageRequest);

  public Orchestration readOrchestration(String appId, String envId, String orchestrationId);

  public PageResponse<WorkflowExecution> listExecutions(
      PageRequest<WorkflowExecution> pageRequest, boolean includeGraph);

  public WorkflowExecution triggerPipelineExecution(String appId, String pipelineId);

  public WorkflowExecution triggerOrchestrationExecution(
      String appId, String orchestrationId, List<String> artifactIds);

  public WorkflowExecution getExecutionDetails(String appId, String workflowExecutionId);
}
