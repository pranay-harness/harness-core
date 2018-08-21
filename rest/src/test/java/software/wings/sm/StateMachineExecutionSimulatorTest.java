/**
 *
 */

package software.wings.sm;

import static com.google.common.collect.Lists.newArrayList;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StateMachine.StateMachineBuilder.aStateMachine;
import static software.wings.sm.Transition.Builder.aTransition;
import static software.wings.sm.states.CommandState.Builder.aCommandState;
import static software.wings.sm.states.RepeatState.Builder.aRepeatState;
import static software.wings.utils.WingsTestConstants.HOST_CONN_ATTR_ID;
import static software.wings.utils.WingsTestConstants.HOST_CONN_ATTR_KEY_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.InstanceElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type State machine execution simulator test.
 *
 * @author Rishi
 */

public class StateMachineExecutionSimulatorTest extends WingsBaseTest {
  private static final SettingAttribute USER_PASS_HOST_CONN_ATTR =
      aSettingAttribute()
          .withUuid(HOST_CONN_ATTR_ID)
          .withValue(aHostConnectionAttributes().withAccessType(AccessType.USER_PASSWORD).build())
          .build();

  private static final SettingAttribute USER_PASS_SUDO_HOST_CONN_ATTR =
      aSettingAttribute()
          .withUuid(HOST_CONN_ATTR_KEY_ID)
          .withValue(aHostConnectionAttributes().withAccessType(AccessType.USER_PASSWORD_SUDO_APP_USER).build())
          .build();

  private static final SettingAttribute USER_PASS_SU_HOST_CONN_ATTR =
      aSettingAttribute()
          .withUuid(HOST_CONN_ATTR_KEY_ID)
          .withValue(aHostConnectionAttributes().withAccessType(AccessType.USER_PASSWORD_SU_APP_USER).build())
          .build();

  @InjectMocks @Inject private StateMachineExecutionSimulator stateMachineExecutionSimulator;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceInstanceService serviceInstanceService;
  @Mock private HostService hostService;
  @Mock private SettingsService settingsService;

  /**
   * Should compute new execution.
   */
  @Test
  public void shouldComputeNewExecution() {
    State s1 = aRepeatState()
                   .withName("ByInstance")
                   .withExecutionStrategy(ExecutionStrategy.SERIAL)
                   .withRepeatTransitionStateName("Install")
                   .withRepeatElementExpression("${instances}")
                   .build();
    State s2 = aCommandState().withName("Install").withCommandName("INSTALL").build();

    StateMachine sm =
        aStateMachine()
            .addState(s1)
            .addState(s2)
            .withInitialStateName(s1.getName())
            .addTransition(
                aTransition().withFromState(s1).withToState(s2).withTransitionType(TransitionType.REPEAT).build())
            .build();

    Application app = anApplication().withName("App1").withUuid(generateUuid()).build();
    Environment env = anEnvironment().withName("DEV").withUuid(generateUuid()).withAppId(app.getUuid()).build();

    ServiceElement service = aServiceElement().withUuid(generateUuid()).withName("service1").build();
    List<InstanceElement> instances =
        Lists.newArrayList(anInstanceElement().withUuid(generateUuid()).withDisplayName("instance1").build(),
            anInstanceElement().withUuid(generateUuid()).withDisplayName("instance2").build());
    stateMachineExecutionSimulator.setExecutionContextFactory(
        new ExecutionContextFactoryTest(app, env, service, instances));

    Map<String, ExecutionStatus> stateExecutionStatuses = new HashMap<>();
    CountsByStatuses breakdown =
        stateMachineExecutionSimulator.getStatusBreakdown(app.getUuid(), env.getUuid(), sm, stateExecutionStatuses);
    assertThat(breakdown)
        .isNotNull()
        .extracting("success", "failed", "inprogress", "queued")
        .containsExactly(0, 0, 0, 2);
  }

  /**
   * Should compute inprogress estimate.
   */
  @Test
  public void shouldComputeInprogressEstimate() {
    State s1 = aRepeatState()
                   .withName("ByService")
                   .withExecutionStrategy(ExecutionStrategy.SERIAL)
                   .withRepeatTransitionStateName("Stop")
                   .withRepeatElementExpression("${services}")
                   .build();
    State s2 = aCommandState().withName("Stop").withCommandName("WAIT").build();
    State s3 = aRepeatState()
                   .withName("ByInstance")
                   .withExecutionStrategy(ExecutionStrategy.SERIAL)
                   .withRepeatTransitionStateName("Install")
                   .withRepeatElementExpression("${instances}")
                   .build();
    State s4 = aCommandState().withName("Install").withCommandName("INSTALL").build();
    State s5 = aCommandState().withName("Start").withCommandName("START").build();

    StateMachine sm =
        aStateMachine()
            .addState(s1)
            .addState(s2)
            .addState(s3)
            .addState(s4)
            .addState(s5)
            .withInitialStateName(s1.getName())
            .addTransition(
                aTransition().withFromState(s2).withToState(s3).withTransitionType(TransitionType.SUCCESS).build())
            .addTransition(
                aTransition().withFromState(s4).withToState(s5).withTransitionType(TransitionType.SUCCESS).build())
            .build();

    Application app = anApplication().withName("App1").withUuid(generateUuid()).build();
    Environment env = anEnvironment().withName("DEV").withUuid(generateUuid()).withAppId(app.getUuid()).build();

    ServiceElement service = aServiceElement().withUuid(generateUuid()).withName("service1").build();
    InstanceElement inst1 = anInstanceElement().withUuid(generateUuid()).withDisplayName("instance1").build();
    InstanceElement inst2 = anInstanceElement().withUuid(generateUuid()).withDisplayName("instance2").build();

    List<InstanceElement> instances = Lists.newArrayList(inst1, inst2);
    stateMachineExecutionSimulator.setExecutionContextFactory(
        new ExecutionContextFactoryTest(app, env, service, instances));

    StateExecutionInstance si1 = aStateExecutionInstance()
                                     .withUuid(generateUuid())
                                     .withDisplayName(s1.getName())
                                     .withStatus(ExecutionStatus.SUCCESS)
                                     .build();
    StateExecutionInstance si2 = aStateExecutionInstance()
                                     .withUuid(generateUuid())
                                     .withDisplayName(s2.getName())
                                     .withParentInstanceId(si1.getUuid())
                                     .withContextElement(aServiceElement().withName(service.getName()).build())
                                     .withStatus(ExecutionStatus.SUCCESS)
                                     .build();
    StateExecutionInstance si3 = aStateExecutionInstance()
                                     .withUuid(generateUuid())
                                     .withDisplayName(s3.getName())
                                     .withParentInstanceId(si1.getUuid())
                                     .withContextElement(aServiceElement().withName(service.getName()).build())
                                     .withStatus(ExecutionStatus.RUNNING)
                                     .build();
    StateExecutionInstance si4 = aStateExecutionInstance()
                                     .withUuid(generateUuid())
                                     .withDisplayName(s4.getName())
                                     .withParentInstanceId(si3.getUuid())
                                     .withContextElement(anInstanceElement().withDisplayName(inst1.getName()).build())
                                     .withStatus(ExecutionStatus.SUCCESS)
                                     .build();
    StateExecutionInstance si5 = aStateExecutionInstance()
                                     .withUuid(generateUuid())
                                     .withDisplayName(s5.getName())
                                     .withParentInstanceId(si3.getUuid())
                                     .withContextElement(anInstanceElement().withDisplayName(inst1.getName()).build())
                                     .withStatus(ExecutionStatus.RUNNING)
                                     .build();

    List<StateExecutionInstance> stateMachineExecutionInstances = newArrayList(si1, si2, si3, si4, si5);
    Map<String, ExecutionStatus> stateExecutionStatuses = new HashMap<>();
    stateMachineExecutionSimulator.prepareStateExecutionInstanceMap(
        stateMachineExecutionInstances.iterator(), stateExecutionStatuses);
    CountsByStatuses breakdown =
        stateMachineExecutionSimulator.getStatusBreakdown(app.getUuid(), env.getUuid(), sm, stateExecutionStatuses);
    assertThat(breakdown)
        .isNotNull()
        .extracting("success", "failed", "inprogress", "queued")
        .containsExactly(2, 0, 1, 2);
  }

  /**
   * Should compute inprogress estimate with failed node.
   */
  @Test
  public void shouldComputeInprogressEstimateWithFailedNode() {
    State s1 = aRepeatState()
                   .withName("ByService")
                   .withExecutionStrategy(ExecutionStrategy.SERIAL)
                   .withRepeatTransitionStateName("wait")
                   .withRepeatElementExpression("${services}")
                   .build();
    State s2 = aCommandState().withName("wait").withCommandName("WAIT").build();
    State s3 = aRepeatState()
                   .withName("ByInstance")
                   .withExecutionStrategy(ExecutionStrategy.SERIAL)
                   .withRepeatTransitionStateName("Install")
                   .withRepeatElementExpression("${instances}")
                   .build();
    State s4 = aCommandState().withName("Install").withCommandName("INSTALL").build();
    State s5 = aCommandState().withName("Start").withCommandName("START").build();
    State s6 = aCommandState().withName("Failed").withCommandName("START").build();

    StateMachine sm =
        aStateMachine()
            .addState(s1)
            .addState(s2)
            .addState(s3)
            .addState(s4)
            .addState(s5)
            .addState(s6)
            .withInitialStateName(s1.getName())
            .addTransition(
                aTransition().withFromState(s2).withToState(s3).withTransitionType(TransitionType.SUCCESS).build())
            .addTransition(
                aTransition().withFromState(s4).withToState(s5).withTransitionType(TransitionType.SUCCESS).build())
            .addTransition(
                aTransition().withFromState(s4).withToState(s6).withTransitionType(TransitionType.FAILURE).build())
            .build();

    Application app = anApplication().withName("App1").withUuid(generateUuid()).build();
    Environment env = anEnvironment().withName("DEV").withUuid(generateUuid()).withAppId(app.getUuid()).build();

    ServiceElement service = aServiceElement().withUuid(generateUuid()).withName("service1").build();
    InstanceElement inst1 = anInstanceElement().withUuid(generateUuid()).withDisplayName("instance1").build();
    InstanceElement inst2 = anInstanceElement().withUuid(generateUuid()).withDisplayName("instance2").build();

    List<InstanceElement> instances = Lists.newArrayList(inst1, inst2);
    stateMachineExecutionSimulator.setExecutionContextFactory(
        new ExecutionContextFactoryTest(app, env, service, instances));

    StateExecutionInstance si1 = aStateExecutionInstance()
                                     .withUuid(generateUuid())
                                     .withDisplayName(s1.getName())
                                     .withStatus(ExecutionStatus.SUCCESS)
                                     .build();
    StateExecutionInstance si2 = aStateExecutionInstance()
                                     .withUuid(generateUuid())
                                     .withDisplayName(s2.getName())
                                     .withParentInstanceId(si1.getUuid())
                                     .withContextElement(aServiceElement().withName(service.getName()).build())
                                     .withStatus(ExecutionStatus.SUCCESS)
                                     .build();
    StateExecutionInstance si3 = aStateExecutionInstance()
                                     .withUuid(generateUuid())
                                     .withDisplayName(s3.getName())
                                     .withParentInstanceId(si1.getUuid())
                                     .withContextElement(aServiceElement().withName(service.getName()).build())
                                     .withStatus(ExecutionStatus.RUNNING)
                                     .build();
    StateExecutionInstance si4 = aStateExecutionInstance()
                                     .withUuid(generateUuid())
                                     .withDisplayName(s4.getName())
                                     .withParentInstanceId(si3.getUuid())
                                     .withContextElement(anInstanceElement().withDisplayName(inst1.getName()).build())
                                     .withStatus(ExecutionStatus.FAILED)
                                     .build();
    StateExecutionInstance si6 = aStateExecutionInstance()
                                     .withUuid(generateUuid())
                                     .withDisplayName(s6.getName())
                                     .withParentInstanceId(si3.getUuid())
                                     .withContextElement(anInstanceElement().withDisplayName(inst1.getName()).build())
                                     .withStatus(ExecutionStatus.SUCCESS)
                                     .build();

    StateExecutionInstance si42 = aStateExecutionInstance()
                                      .withUuid(generateUuid())
                                      .withDisplayName(s4.getName())
                                      .withParentInstanceId(si3.getUuid())
                                      .withContextElement(anInstanceElement().withDisplayName(inst2.getName()).build())
                                      .withStatus(ExecutionStatus.SUCCESS)
                                      .build();
    StateExecutionInstance si52 = aStateExecutionInstance()
                                      .withUuid(generateUuid())
                                      .withDisplayName(s5.getName())
                                      .withParentInstanceId(si3.getUuid())
                                      .withContextElement(anInstanceElement().withDisplayName(inst2.getName()).build())
                                      .withStatus(ExecutionStatus.RUNNING)
                                      .build();

    List<StateExecutionInstance> stateMachineExecutionInstances = newArrayList(si1, si2, si3, si4, si6, si42, si52);
    Map<String, ExecutionStatus> stateExecutionStatuses = new HashMap<>();
    stateMachineExecutionSimulator.prepareStateExecutionInstanceMap(
        stateMachineExecutionInstances.iterator(), stateExecutionStatuses);
    CountsByStatuses breakdown =
        stateMachineExecutionSimulator.getStatusBreakdown(app.getUuid(), env.getUuid(), sm, stateExecutionStatuses);
    assertThat(breakdown)
        .isNotNull()
        .extracting("success", "failed", "inprogress", "queued")
        .containsExactly(3, 1, 1, 0);
  }

  /**
   * The type Execution context factory test.
   */
  public static class ExecutionContextFactoryTest extends ExecutionContextFactory {
    private final Application app;
    private final Environment env;
    private final ServiceElement service;
    private final List<InstanceElement> instances;

    /**
     * Instantiates a new Execution context factory test.
     *
     * @param app       the app
     * @param env       the env
     * @param service   the service
     * @param instances the instances
     */
    public ExecutionContextFactoryTest(
        Application app, Environment env, ServiceElement service, List<InstanceElement> instances) {
      this.app = app;
      this.env = env;
      this.service = service;
      this.instances = instances;
    }

    @Override
    public ExecutionContext createExecutionContext(
        StateExecutionInstance stateExecutionInstance, StateMachine stateMachine) {
      ExecutionContextImpl context = spy(new ExecutionContextImpl(stateExecutionInstance));
      doReturn(newArrayList(service)).when(context).evaluateExpression("${services}");
      doReturn(instances).when(context).evaluateExpression("${instances}");
      doReturn(service).when(context).getContextElement(ContextElementType.SERVICE);
      doReturn(app).when(context).getApp();
      doReturn(env).when(context).getEnv();
      return context;
    }
  }
}
