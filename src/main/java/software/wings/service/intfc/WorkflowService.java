/**
 *
 */

package software.wings.service.intfc;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutionCallback;
import software.wings.sm.StateTypeDescriptor;

/**
 * @author Rishi
 */
public interface WorkflowService {
  public StateMachine create(StateMachine stateMachine);

  public StateMachine update(StateMachine stateMachine);

  public StateMachine read(String smId);

  public void trigger(String smId);

  public void trigger(String smId, Map<String, Serializable> arguments);

  public void trigger(String smId, Map<String, Serializable> arguments, StateMachineExecutionCallback callback);

  public List<StateTypeDescriptor> stencils();
}
