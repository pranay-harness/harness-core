/**
 *
 */

package software.wings.service.impl;

import com.google.inject.Singleton;

import ro.fortsoft.pf4j.PluginManager;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutionCallback;
import software.wings.sm.StateMachineExecutor;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * @author Rishi
 */
@Singleton
public class WorkflowServiceImpl implements WorkflowService {
  @Inject private WingsPersistence wingsPersistence;

  @Inject private StateMachineExecutor stateMachineExecutor;

  @Inject private PluginManager pluginManager;

  /*
   * (non-Javadoc)
   *
   * @see software.wings.service.intfc.WorkflowService#create(software.wings.sm.StateMachine)
   */
  @Override
  public StateMachine create(StateMachine stateMachine) {
    stateMachine.validate();
    return wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
  }

  /*
   * (non-Javadoc)
   *
   * @see software.wings.service.intfc.WorkflowService#update(software.wings.sm.StateMachine)
   */
  @Override
  public StateMachine update(StateMachine stateMachine) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see software.wings.service.intfc.WorkflowService#read(software.wings.sm.StateMachine)
   */
  @Override
  public StateMachine read(String smId) {
    return wingsPersistence.get(StateMachine.class, smId);
  }

  @Override
  public void trigger(String smId) {
    trigger(smId, null);
  }

  @Override
  public void trigger(String smId, Map<String, Serializable> arguments) {
    trigger(smId, arguments, null);
  }

  @Override
  public void trigger(String smId, Map<String, Serializable> arguments, StateMachineExecutionCallback callback) {
    stateMachineExecutor.execute(smId, arguments);
  }

  @Override
  public List<StateTypeDescriptor> stencils() {
    List<StateTypeDescriptor> stencils = new ArrayList<StateTypeDescriptor>();
    stencils.addAll(Arrays.asList(StateType.values()));

    List<StateTypeDescriptor> plugins = pluginManager.getExtensions(StateTypeDescriptor.class);
    stencils.addAll(plugins);
    return stencils;
  }
}
