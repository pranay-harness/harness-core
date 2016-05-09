/**
 *
 */

package software.wings.service.impl;

import com.google.inject.Singleton;

import ro.fortsoft.pf4j.PluginManager;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Pipeline;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutor;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

/**
 * @author Rishi
 */
@Singleton
@ValidateOnExecution
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
  public StateMachine create(@Valid StateMachine stateMachine) {
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
    stateMachineExecutor.execute(smId);
  }

  @Override
  public List<StateTypeDescriptor> stencils() {
    List<StateTypeDescriptor> stencils = new ArrayList<StateTypeDescriptor>();
    stencils.addAll(Arrays.asList(StateType.values()));

    List<StateTypeDescriptor> plugins = pluginManager.getExtensions(StateTypeDescriptor.class);
    stencils.addAll(plugins);
    return stencils;
  }

  @Override
  public PageResponse<Pipeline> listPipeline(PageRequest<Pipeline> req) {
    return null;
  }

  @Override
  public Pipeline createPipeline(Pipeline pipeline) {
    return wingsPersistence.saveAndGet(Pipeline.class, pipeline);
  }

  @Override
  public Pipeline updatePipeline(Pipeline pipeline) {
    // create a new version of state machine
    return null;
  }

  @Override
  public Pipeline readPipeline(String applicationId, String pipelineId) {
    return wingsPersistence.get(Pipeline.class, applicationId, pipelineId);
  }
}
