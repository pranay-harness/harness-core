/**
 *
 */
package software.wings.sm;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Host.Builder.aHost;
import static software.wings.beans.HostConnectionAttributes.HostConnectionAttributesBuilder.aHostConnectionAttributes;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StateMachine.Builder.aStateMachine;
import static software.wings.sm.Transition.Builder.aTransition;
import static software.wings.sm.states.CommandState.Builder.aCommandState;
import static software.wings.sm.states.RepeatState.Builder.aRepeatState;
import static software.wings.sm.states.WaitState.Builder.aWaitState;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.InstanceElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.ServiceInstance;
import software.wings.beans.command.Command;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;
import javax.inject.Inject;

/**
 * @author Rishi
 */

public class StateMachineExecutionSimulatorTest extends WingsBaseTest {
  @InjectMocks @Inject private StateMachineExecutionSimulator stateMachineExecutionSimulator;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceInstanceService serviceInstanceService;

  @Test
  public void shouldReturnEmptyArgType() {
    State s1 = aRepeatState()
                   .withName("ByService")
                   .withExecutionStrategy(ExecutionStrategy.SERIAL)
                   .withRepeatTransitionStateName("wait1")
                   .withRepeatElementExpression("${services}")
                   .build();
    State s2 = aWaitState().withName("wait1").withDuration(1).build();

    StateMachine sm =
        aStateMachine()
            .addState(s1)
            .addState(s2)
            .withInitialStateName(s1.getName())
            .addTransition(
                aTransition().withFromState(s1).withToState(s2).withTransitionType(TransitionType.REPEAT).build())
            .build();

    Application app = anApplication().withName("App1").withUuid(getUuid()).build();
    Environment env = anEnvironment().withName("DEV").withUuid(getUuid()).withAppId(app.getUuid()).build();

    ServiceElement service = aServiceElement().withUuid(getUuid()).withName("service1").build();
    stateMachineExecutionSimulator.setExecutionContextFactory(new ExecutionContextFactoryTest(app, env, service, null));

    ExecutionArgs executionArgs = new ExecutionArgs();

    RequiredExecutionArgs reqArgs =
        stateMachineExecutionSimulator.getRequiredExecutionArgs(app.getUuid(), env.getUuid(), sm, executionArgs);
    assertThat(reqArgs).isNotNull();
    assertThat(reqArgs.getEntityTypes()).isNotNull().hasSize(0);
  }

  @Test
  public void shouldReturnSshArgType() {
    State s1 = aRepeatState()
                   .withName("ByInstance")
                   .withExecutionStrategy(ExecutionStrategy.SERIAL)
                   .withRepeatTransitionStateName("stop1")
                   .withRepeatElementExpression("${instances}")
                   .build();
    State s2 = aCommandState().withName("stop1").withCommandName("STOP").build();

    StateMachine sm =
        aStateMachine()
            .addState(s1)
            .addState(s2)
            .withInitialStateName(s1.getName())
            .addTransition(
                aTransition().withFromState(s1).withToState(s2).withTransitionType(TransitionType.REPEAT).build())
            .build();

    Application app = anApplication().withName("App1").withUuid(getUuid()).build();
    Environment env = anEnvironment().withName("DEV").withUuid(getUuid()).withAppId(app.getUuid()).build();

    ServiceElement service = aServiceElement().withUuid(getUuid()).withName("service1").build();
    List<InstanceElement> instances =
        Lists.newArrayList(anInstanceElement().withUuid(getUuid()).withDisplayName("instance1").build(),
            anInstanceElement().withUuid(getUuid()).withDisplayName("instance2").build());
    stateMachineExecutionSimulator.setExecutionContextFactory(
        new ExecutionContextFactoryTest(app, env, service, instances));

    ExecutionArgs executionArgs = new ExecutionArgs();

    Command cmd = mock(Command.class);
    when(cmd.isArtifactNeeded()).thenReturn(false);
    when(serviceResourceService.getCommandByName(app.getUuid(), service.getUuid(), "STOP")).thenReturn(cmd);

    PageResponse<ServiceInstance> res = new PageResponse<>();
    res.setResponse(Lists.newArrayList(
        aServiceInstance()
            .withUuid(getUuid())
            .withHost(
                aHost()
                    .withHostConnAttr(
                        aSettingAttribute()
                            .withValue(aHostConnectionAttributes().withAccessType(AccessType.USER_PASSWORD).build())
                            .build())
                    .build())
            .build()));
    when(serviceInstanceService.list(anyObject())).thenReturn(res);

    RequiredExecutionArgs reqArgs =
        stateMachineExecutionSimulator.getRequiredExecutionArgs(app.getUuid(), env.getUuid(), sm, executionArgs);
    assertThat(reqArgs).isNotNull();
    assertThat(reqArgs.getEntityTypes())
        .isNotNull()
        .hasSize(2)
        .containsExactlyInAnyOrder(EntityType.SSH_USER, EntityType.SSH_PASSWORD);
  }

  @Test
  public void shouldReturnSshArtifactArgType() {
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

    Application app = anApplication().withName("App1").withUuid(getUuid()).build();
    Environment env = anEnvironment().withName("DEV").withUuid(getUuid()).withAppId(app.getUuid()).build();

    ServiceElement service = aServiceElement().withUuid(getUuid()).withName("service1").build();
    List<InstanceElement> instances =
        Lists.newArrayList(anInstanceElement().withUuid(getUuid()).withDisplayName("instance1").build(),
            anInstanceElement().withUuid(getUuid()).withDisplayName("instance2").build());
    stateMachineExecutionSimulator.setExecutionContextFactory(
        new ExecutionContextFactoryTest(app, env, service, instances));

    ExecutionArgs executionArgs = new ExecutionArgs();

    Command cmd = mock(Command.class);
    when(cmd.isArtifactNeeded()).thenReturn(true);
    when(serviceResourceService.getCommandByName(app.getUuid(), service.getUuid(), "INSTALL")).thenReturn(cmd);

    PageResponse<ServiceInstance> res = new PageResponse<>();
    res.setResponse(Lists.newArrayList(
        aServiceInstance()
            .withUuid(getUuid())
            .withHost(
                aHost()
                    .withHostConnAttr(
                        aSettingAttribute()
                            .withValue(aHostConnectionAttributes().withAccessType(AccessType.USER_PASSWORD).build())
                            .build())
                    .build())
            .build()));
    when(serviceInstanceService.list(anyObject())).thenReturn(res);

    RequiredExecutionArgs reqArgs =
        stateMachineExecutionSimulator.getRequiredExecutionArgs(app.getUuid(), env.getUuid(), sm, executionArgs);
    assertThat(reqArgs).isNotNull();
    assertThat(reqArgs.getEntityTypes())
        .isNotNull()
        .hasSize(3)
        .containsExactlyInAnyOrder(EntityType.SSH_USER, EntityType.SSH_PASSWORD, EntityType.ARTIFACT);
  }

  @Test
  public void shouldReturnSshSUArtifactArgType() {
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

    Application app = anApplication().withName("App1").withUuid(getUuid()).build();
    Environment env = anEnvironment().withName("DEV").withUuid(getUuid()).withAppId(app.getUuid()).build();

    ServiceElement service = aServiceElement().withUuid(getUuid()).withName("service1").build();
    List<InstanceElement> instances =
        Lists.newArrayList(anInstanceElement().withUuid(getUuid()).withDisplayName("instance1").build(),
            anInstanceElement().withUuid(getUuid()).withDisplayName("instance2").build());
    stateMachineExecutionSimulator.setExecutionContextFactory(
        new ExecutionContextFactoryTest(app, env, service, instances));

    ExecutionArgs executionArgs = new ExecutionArgs();

    Command cmd = mock(Command.class);
    when(cmd.isArtifactNeeded()).thenReturn(true);
    when(serviceResourceService.getCommandByName(app.getUuid(), service.getUuid(), "INSTALL")).thenReturn(cmd);

    PageResponse<ServiceInstance> res = new PageResponse<>();
    res.setResponse(Lists.newArrayList(
        aServiceInstance()
            .withUuid(getUuid())
            .withHost(
                aHost()
                    .withHostConnAttr(
                        aSettingAttribute()
                            .withValue(aHostConnectionAttributes().withAccessType(AccessType.USER_PASSWORD).build())
                            .build())
                    .build())
            .build(),
        aServiceInstance()
            .withUuid(getUuid())
            .withHost(aHost()
                          .withHostConnAttr(aSettingAttribute()
                                                .withValue(aHostConnectionAttributes()
                                                               .withAccessType(AccessType.USER_PASSWORD_SU_APP_USER)
                                                               .build())
                                                .build())
                          .build())
            .build()));
    when(serviceInstanceService.list(anyObject())).thenReturn(res);

    RequiredExecutionArgs reqArgs =
        stateMachineExecutionSimulator.getRequiredExecutionArgs(app.getUuid(), env.getUuid(), sm, executionArgs);
    assertThat(reqArgs).isNotNull();
    assertThat(reqArgs.getEntityTypes())
        .isNotNull()
        .hasSize(5)
        .containsExactlyInAnyOrder(EntityType.ARTIFACT, EntityType.SSH_USER, EntityType.SSH_PASSWORD,
            EntityType.SSH_APP_ACCOUNT, EntityType.SSH_APP_ACCOUNT_PASSOWRD);
  }

  @Test
  public void shouldReturnSshSUDOArtifactArgType() {
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

    Application app = anApplication().withName("App1").withUuid(getUuid()).build();
    Environment env = anEnvironment().withName("DEV").withUuid(getUuid()).withAppId(app.getUuid()).build();

    ServiceElement service = aServiceElement().withUuid(getUuid()).withName("service1").build();
    List<InstanceElement> instances =
        Lists.newArrayList(anInstanceElement().withUuid(getUuid()).withDisplayName("instance1").build(),
            anInstanceElement().withUuid(getUuid()).withDisplayName("instance2").build());
    stateMachineExecutionSimulator.setExecutionContextFactory(
        new ExecutionContextFactoryTest(app, env, service, instances));

    ExecutionArgs executionArgs = new ExecutionArgs();

    Command cmd = mock(Command.class);
    when(cmd.isArtifactNeeded()).thenReturn(true);
    when(serviceResourceService.getCommandByName(app.getUuid(), service.getUuid(), "INSTALL")).thenReturn(cmd);

    PageResponse<ServiceInstance> res = new PageResponse<>();
    res.setResponse(Lists.newArrayList(
        aServiceInstance()
            .withUuid(getUuid())
            .withHost(
                aHost()
                    .withHostConnAttr(
                        aSettingAttribute()
                            .withValue(aHostConnectionAttributes().withAccessType(AccessType.USER_PASSWORD).build())
                            .build())
                    .build())
            .build(),
        aServiceInstance()
            .withUuid(getUuid())
            .withHost(aHost()
                          .withHostConnAttr(aSettingAttribute()
                                                .withValue(aHostConnectionAttributes()
                                                               .withAccessType(AccessType.USER_PASSWORD_SUDO_APP_USER)
                                                               .build())
                                                .build())
                          .build())
            .build()));
    when(serviceInstanceService.list(anyObject())).thenReturn(res);

    RequiredExecutionArgs reqArgs =
        stateMachineExecutionSimulator.getRequiredExecutionArgs(app.getUuid(), env.getUuid(), sm, executionArgs);
    assertThat(reqArgs).isNotNull();
    assertThat(reqArgs.getEntityTypes())
        .isNotNull()
        .hasSize(4)
        .containsExactlyInAnyOrder(
            EntityType.ARTIFACT, EntityType.SSH_USER, EntityType.SSH_PASSWORD, EntityType.SSH_APP_ACCOUNT);
  }

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

    Application app = anApplication().withName("App1").withUuid(getUuid()).build();
    Environment env = anEnvironment().withName("DEV").withUuid(getUuid()).withAppId(app.getUuid()).build();

    ServiceElement service = aServiceElement().withUuid(getUuid()).withName("service1").build();
    List<InstanceElement> instances =
        Lists.newArrayList(anInstanceElement().withUuid(getUuid()).withDisplayName("instance1").build(),
            anInstanceElement().withUuid(getUuid()).withDisplayName("instance2").build());
    stateMachineExecutionSimulator.setExecutionContextFactory(
        new ExecutionContextFactoryTest(app, env, service, instances));

    CountsByStatuses breakdown =
        stateMachineExecutionSimulator.getStatusBreakdown(app.getUuid(), env.getUuid(), sm, null);
    assertThat(breakdown).isNotNull().extracting("success", "failed", "inprogress").containsExactly(0, 0, 2);
  }

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

    Application app = anApplication().withName("App1").withUuid(getUuid()).build();
    Environment env = anEnvironment().withName("DEV").withUuid(getUuid()).withAppId(app.getUuid()).build();

    ServiceElement service = aServiceElement().withUuid(getUuid()).withName("service1").build();
    InstanceElement inst1 = anInstanceElement().withUuid(getUuid()).withDisplayName("instance1").build();
    InstanceElement inst2 = anInstanceElement().withUuid(getUuid()).withDisplayName("instance2").build();

    List<InstanceElement> instances = Lists.newArrayList(inst1, inst2);
    stateMachineExecutionSimulator.setExecutionContextFactory(
        new ExecutionContextFactoryTest(app, env, service, instances));

    StateExecutionInstance si1 = aStateExecutionInstance()
                                     .withUuid(getUuid())
                                     .withStateName(s1.getName())
                                     .withStatus(ExecutionStatus.SUCCESS)
                                     .build();
    StateExecutionInstance si2 = aStateExecutionInstance()
                                     .withUuid(getUuid())
                                     .withStateName(s2.getName())
                                     .withParentInstanceId(si1.getUuid())
                                     .withContextElementType(ContextElementType.SERVICE.name())
                                     .withContextElementName(service.getName())
                                     .withStatus(ExecutionStatus.SUCCESS)
                                     .build();
    StateExecutionInstance si3 = aStateExecutionInstance()
                                     .withUuid(getUuid())
                                     .withStateName(s3.getName())
                                     .withParentInstanceId(si1.getUuid())
                                     .withContextElementType(ContextElementType.SERVICE.name())
                                     .withContextElementName(service.getName())
                                     .withStatus(ExecutionStatus.RUNNING)
                                     .build();
    StateExecutionInstance si4 = aStateExecutionInstance()
                                     .withUuid(getUuid())
                                     .withStateName(s4.getName())
                                     .withParentInstanceId(si3.getUuid())
                                     .withContextElementType(ContextElementType.INSTANCE.name())
                                     .withContextElementName(inst1.getName())
                                     .withStatus(ExecutionStatus.SUCCESS)
                                     .build();

    List<StateExecutionInstance> stateMachineExecutionInstances = newArrayList(si1, si2, si3, si4);
    CountsByStatuses breakdown = stateMachineExecutionSimulator.getStatusBreakdown(
        app.getUuid(), env.getUuid(), sm, stateMachineExecutionInstances);
    assertThat(breakdown).isNotNull().extracting("success", "failed", "inprogress").containsExactly(2, 0, 3);
  }

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

    Application app = anApplication().withName("App1").withUuid(getUuid()).build();
    Environment env = anEnvironment().withName("DEV").withUuid(getUuid()).withAppId(app.getUuid()).build();

    ServiceElement service = aServiceElement().withUuid(getUuid()).withName("service1").build();
    InstanceElement inst1 = anInstanceElement().withUuid(getUuid()).withDisplayName("instance1").build();
    InstanceElement inst2 = anInstanceElement().withUuid(getUuid()).withDisplayName("instance2").build();

    List<InstanceElement> instances = Lists.newArrayList(inst1, inst2);
    stateMachineExecutionSimulator.setExecutionContextFactory(
        new ExecutionContextFactoryTest(app, env, service, instances));

    StateExecutionInstance si1 = aStateExecutionInstance()
                                     .withUuid(getUuid())
                                     .withStateName(s1.getName())
                                     .withStatus(ExecutionStatus.SUCCESS)
                                     .build();
    StateExecutionInstance si2 = aStateExecutionInstance()
                                     .withUuid(getUuid())
                                     .withStateName(s2.getName())
                                     .withParentInstanceId(si1.getUuid())
                                     .withContextElementType(ContextElementType.SERVICE.name())
                                     .withContextElementName(service.getName())
                                     .withStatus(ExecutionStatus.SUCCESS)
                                     .build();
    StateExecutionInstance si3 = aStateExecutionInstance()
                                     .withUuid(getUuid())
                                     .withStateName(s3.getName())
                                     .withParentInstanceId(si1.getUuid())
                                     .withContextElementType(ContextElementType.SERVICE.name())
                                     .withContextElementName(service.getName())
                                     .withStatus(ExecutionStatus.RUNNING)
                                     .build();
    StateExecutionInstance si4 = aStateExecutionInstance()
                                     .withUuid(getUuid())
                                     .withStateName(s4.getName())
                                     .withParentInstanceId(si3.getUuid())
                                     .withContextElementType(ContextElementType.INSTANCE.name())
                                     .withContextElementName(inst1.getName())
                                     .withStatus(ExecutionStatus.FAILED)
                                     .build();
    StateExecutionInstance si6 = aStateExecutionInstance()
                                     .withUuid(getUuid())
                                     .withStateName(s6.getName())
                                     .withParentInstanceId(si3.getUuid())
                                     .withContextElementType(ContextElementType.INSTANCE.name())
                                     .withContextElementName(inst1.getName())
                                     .withStatus(ExecutionStatus.SUCCESS)
                                     .build();

    StateExecutionInstance si42 = aStateExecutionInstance()
                                      .withUuid(getUuid())
                                      .withStateName(s4.getName())
                                      .withParentInstanceId(si3.getUuid())
                                      .withContextElementType(ContextElementType.INSTANCE.name())
                                      .withContextElementName(inst2.getName())
                                      .withStatus(ExecutionStatus.SUCCESS)
                                      .build();
    StateExecutionInstance si52 = aStateExecutionInstance()
                                      .withUuid(getUuid())
                                      .withStateName(s5.getName())
                                      .withParentInstanceId(si3.getUuid())
                                      .withContextElementType(ContextElementType.INSTANCE.name())
                                      .withContextElementName(inst1.getName())
                                      .withStatus(ExecutionStatus.RUNNING)
                                      .build();

    List<StateExecutionInstance> stateMachineExecutionInstances = newArrayList(si1, si2, si3, si4, si6, si42, si52);
    CountsByStatuses breakdown = stateMachineExecutionSimulator.getStatusBreakdown(
        app.getUuid(), env.getUuid(), sm, stateMachineExecutionInstances);
    assertThat(breakdown).isNotNull().extracting("success", "failed", "inprogress").containsExactly(3, 1, 1);
  }

  public static class ExecutionContextFactoryTest extends ExecutionContextFactory {
    private final Application app;
    private final Environment env;
    private final ServiceElement service;
    private final List<InstanceElement> instances;

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
