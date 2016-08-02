/**
 *
 */
package software.wings.sm;

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
import static software.wings.beans.Service.Builder.aService;
import static software.wings.common.UUIDGenerator.getUuid;
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
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.Service;
import software.wings.beans.command.Command;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ServiceResourceService;

import javax.inject.Inject;

/**
 * @author Rishi
 *
 */

public class StateMachineExecutionSimulatorTest extends WingsBaseTest {
  @InjectMocks @Inject private StateMachineExecutionSimulator stateMachineExecutionSimulator;
  @Mock private ServiceResourceService serviceResourceService;

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
            .addTransition(
                aTransition().withFromState(s1).withToState(s2).withTransitionType(TransitionType.REPEAT).build())
            .build();

    Application app = anApplication().withUuid(getUuid()).build();
    Environment env = anEnvironment().withUuid(getUuid()).build();

    ExecutionArgs executionArgs = new ExecutionArgs();

    PageResponse<Service> res = new PageResponse<>();
    res.setResponse(
        Lists.newArrayList(aService().withUuid(getUuid()).withAppId(app.getUuid()).withName("service1").build()));
    when(serviceResourceService.list(anyObject())).thenReturn(res);
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
            .withInitialStateName("ByInstance")
            .addTransition(
                aTransition().withFromState(s1).withToState(s2).withTransitionType(TransitionType.REPEAT).build())
            .build();

    Application app = anApplication().withName("App1").withUuid(getUuid()).build();
    Environment env = anEnvironment().withName("DEV").withUuid(getUuid()).withAppId(app.getUuid()).build();
    Service service = aService().withUuid(getUuid()).withAppId(app.getUuid()).withName("service1").build();

    stateMachineExecutionSimulator.setExecutionContextFactory(new ExecutionContextFactoryTest(app, service, env));

    ExecutionArgs executionArgs = new ExecutionArgs();

    PageResponse<Service> res = new PageResponse<>();
    res.setResponse(Lists.newArrayList(service));
    when(serviceResourceService.list(anyObject())).thenReturn(res);
    Command cmd = mock(Command.class);
    when(cmd.isArtifactNeeded()).thenReturn(false);
    when(serviceResourceService.getCommandByName(app.getUuid(), service.getUuid(), "STOP")).thenReturn(cmd);

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
            .withInitialStateName("ByInstance")
            .addTransition(
                aTransition().withFromState(s1).withToState(s2).withTransitionType(TransitionType.REPEAT).build())
            .build();

    Application app = anApplication().withName("App1").withUuid(getUuid()).build();
    Environment env = anEnvironment().withName("DEV").withUuid(getUuid()).withAppId(app.getUuid()).build();
    Service service = aService().withUuid(getUuid()).withAppId(app.getUuid()).withName("service1").build();

    stateMachineExecutionSimulator.setExecutionContextFactory(new ExecutionContextFactoryTest(app, service, env));

    ExecutionArgs executionArgs = new ExecutionArgs();

    PageResponse<Service> res = new PageResponse<>();
    res.setResponse(Lists.newArrayList(service));
    when(serviceResourceService.list(anyObject())).thenReturn(res);
    Command cmd = mock(Command.class);
    when(cmd.isArtifactNeeded()).thenReturn(true);
    when(serviceResourceService.getCommandByName(app.getUuid(), service.getUuid(), "INSTALL")).thenReturn(cmd);

    RequiredExecutionArgs reqArgs =
        stateMachineExecutionSimulator.getRequiredExecutionArgs(app.getUuid(), env.getUuid(), sm, executionArgs);
    assertThat(reqArgs).isNotNull();
    assertThat(reqArgs.getEntityTypes())
        .isNotNull()
        .hasSize(3)
        .containsExactlyInAnyOrder(EntityType.SSH_USER, EntityType.SSH_PASSWORD, EntityType.ARTIFACT);
  }

  static class ExecutionContextFactoryTest extends ExecutionContextFactory {
    private final Application app;
    private final Service service;
    private final Environment env;

    public ExecutionContextFactoryTest(Application app, Service service, Environment env) {
      this.app = app;
      this.service = service;
      this.env = env;
    }

    @Override
    public ExecutionContext createExecutionContext(
        StateExecutionInstance stateExecutionInstance, StateMachine stateMachine) {
      ExecutionContextImpl context = spy(new ExecutionContextImpl(stateExecutionInstance));
      doReturn(Lists.newArrayList(anInstanceElement().withUuid(getUuid()).withDisplayName("Instance1").build()))
          .when(context)
          .evaluateExpression("${instances}");
      doReturn(aServiceElement().withUuid(service.getUuid()).withName(service.getName()).build())
          .when(context)
          .getContextElement(ContextElementType.SERVICE);
      doReturn(app).when(context).getApp();
      doReturn(env).when(context).getEnv();
      return context;
    }
  }
}
