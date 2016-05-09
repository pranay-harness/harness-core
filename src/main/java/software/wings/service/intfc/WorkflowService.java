package software.wings.service.intfc;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Pipeline;
import software.wings.sm.StateMachine;
import software.wings.sm.StateTypeDescriptor;

import java.util.List;

import javax.validation.Valid;

/**
 * @author Rishi
 */
public interface WorkflowService {
  public PageResponse<Pipeline> listPipeline(PageRequest<Pipeline> req);

  public Pipeline createPipeline(Pipeline pipeline);

  public Pipeline updatePipeline(Pipeline pipeline);

  public Pipeline readPipeline(String appId, String pipelineId);

  public StateMachine create(@Valid StateMachine stateMachine);

  public StateMachine update(StateMachine stateMachine);

  public StateMachine read(String smId);

  public void trigger(String smId);

  //  public void trigger(String smId, Map<String, Serializable> arguments);

  //  public void trigger(String smId, Map<String, Serializable> arguments,
  //      StateMachineExecutionCallback callback);

  public List<StateTypeDescriptor> stencils();
}
