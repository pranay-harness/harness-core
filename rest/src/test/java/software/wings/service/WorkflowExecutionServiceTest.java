package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.Orchestration.Builder.anOrchestration;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.common.UUIDGenerator.getUuid;

import com.google.common.collect.Sets;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Graph;
import software.wings.beans.Orchestration;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.WorkflowType;
import software.wings.beans.command.Command;
import software.wings.common.UUIDGenerator;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateMachineExecutionSimulator;
import software.wings.sm.StateType;
import software.wings.waitnotify.NotifyEventListener;

import java.util.ArrayList;
import javax.inject.Inject;

/**
 * The Class workflowExecutionServiceTest.
 *
 * @author Rishi
 */
@Listeners(NotifyEventListener.class)
public class WorkflowExecutionServiceTest extends WingsBaseTest {
  private static String appId = UUIDGenerator.getUuid();
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @InjectMocks @Inject private WorkflowExecutionService workflowExecutionService;

  @Inject private WorkflowService workflowService;

  @Inject private WingsPersistence wingsPersistence;

  @Mock private ServiceResourceService serviceResourceServiceMock;
  @Mock private ServiceInstanceService serviceInstanceServiceMock;
  @Mock private StateMachineExecutionSimulator stateMachineExecutionSimulator;

  /**
   * Required execution args for simple workflow start.
   */
  @Test
  public void requiredExecutionArgsForSimpleWorkflowStart() {
    Application app = anApplication().withName("App1").withUuid(getUuid()).build();
    Environment env = anEnvironment().withName("DEV").withUuid(getUuid()).withAppId(app.getUuid()).build();

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.SIMPLE);

    String serviceId = UUIDGenerator.getUuid();
    executionArgs.setServiceId(serviceId);

    String commandName = "Start";
    executionArgs.setCommandName(commandName);

    ServiceInstance inst1 = ServiceInstance.Builder.aServiceInstance().withUuid(UUIDGenerator.getUuid()).build();
    ServiceInstance inst2 = ServiceInstance.Builder.aServiceInstance().withUuid(UUIDGenerator.getUuid()).build();
    executionArgs.setServiceInstances(Lists.newArrayList(inst1, inst2));

    when(stateMachineExecutionSimulator.getInfrastructureRequiredEntityType(
             app.getUuid(), Lists.newArrayList(inst1.getUuid(), inst2.getUuid())))
        .thenReturn(Sets.newHashSet(EntityType.SSH_USER, EntityType.SSH_PASSWORD));

    Command cmd = mock(Command.class);
    when(cmd.isArtifactNeeded()).thenReturn(false);
    when(serviceResourceServiceMock.getCommandByName(app.getUuid(), serviceId, env.getUuid(), "Start"))
        .thenReturn(aServiceCommand().withCommand(cmd).build());

    RequiredExecutionArgs required =
        workflowExecutionService.getRequiredExecutionArgs(app.getUuid(), env.getUuid(), executionArgs);
    assertThat(required).isNotNull();
    assertThat(required.getEntityTypes()).isNotNull().hasSize(2).contains(EntityType.SSH_USER, EntityType.SSH_PASSWORD);
  }

  /**
   * Required execution args for orchestrated workflow.
   */
  @Test
  public void requiredExecutionArgsForOrchestratedWorkflow() {
    Environment env = wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(appId).build());
    Graph graph = aGraph()
                      .addNodes(aNode()
                                    .withId("n2")
                                    .withOrigin(true)
                                    .withName("wait")
                                    .withX(250)
                                    .withY(50)
                                    .withType(StateType.WAIT.name())
                                    .addProperty("duration", 1l)
                                    .build())
                      .build();

    ArrayList<Service> services = Lists.newArrayList(
        wingsPersistence.saveAndGet(Service.class, aService().withAppId(appId).withName("catalog").build()),
        wingsPersistence.saveAndGet(Service.class, aService().withAppId(appId).withName("content").build()));

    Orchestration orchestration = anOrchestration()
                                      .withAppId(appId)
                                      .withName("workflow1")
                                      .withDescription("Sample Workflow")
                                      .withGraph(graph)
                                      .withServices(services)
                                      .withTargetToAllEnv(true)
                                      .build();

    orchestration = workflowService.createWorkflow(Orchestration.class, orchestration);
    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    executionArgs.setOrchestrationId(orchestration.getUuid());

    RequiredExecutionArgs requiredExecutionArgs = new RequiredExecutionArgs();
    requiredExecutionArgs.setEntityTypes(Sets.newHashSet(EntityType.SSH_USER, EntityType.SSH_PASSWORD));

    when(stateMachineExecutionSimulator.getRequiredExecutionArgs(anyObject(), anyObject(), anyObject(), anyObject()))
        .thenReturn(requiredExecutionArgs);

    RequiredExecutionArgs required =
        workflowExecutionService.getRequiredExecutionArgs(appId, env.getUuid(), executionArgs);
    assertThat(required).isNotNull().isEqualTo(requiredExecutionArgs);
  }
}
