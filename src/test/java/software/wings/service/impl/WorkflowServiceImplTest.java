/**
 *
 */

package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.Host.HostBuilder.aHost;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate;
import static software.wings.utils.WingsTestConstants.INFRA_ID;

import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.StaticConfiguration;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentBuilder;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.Graph;
import software.wings.beans.Host;
import software.wings.beans.Orchestration;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance.Builder;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.common.UUIDGenerator;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.waitnotify.NotifyEventListener;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * The type Workflow service impl test.
 *
 * @author Rishi
 */
@Listeners(NotifyEventListener.class)
public class WorkflowServiceImplTest extends WingsBaseTest {
  private static String appId = UUIDGenerator.getUuid();
  @Inject private WorkflowService workflowService;
  @Inject private WingsPersistence wingsPersistence;
  @Mock @Inject private StaticConfiguration staticConfiguration;

  @Inject private ServiceInstanceService serviceInstanceService;

  private Environment env;

  /**
   * Gets environment.
   *
   * @return the environment
   */
  public Environment getEnvironment() {
    if (env == null) {
      env = wingsPersistence.saveAndGet(Environment.class, EnvironmentBuilder.anEnvironment().withAppId(appId).build());
    }
    return env;
  }

  /**
   * Should read simple workflow.
   */
  @Test
  public void shouldReadSimpleWorkflow() {
    WorkflowServiceImpl impl = (WorkflowServiceImpl) workflowService;
    env = getEnvironment();
    Orchestration workflow = impl.readLatestSimpleWorkflow(appId, env.getUuid());
    assertThat(workflow).isNotNull();
    assertThat(workflow.getWorkflowType()).isEqualTo(WorkflowType.SIMPLE);
    assertThat(workflow.getGraph()).isNotNull();
    assertThat(workflow.getGraph().getNodes()).isNotNull();
    assertThat(workflow.getGraph().getNodes().size()).isEqualTo(3);
    assertThat(workflow.getGraph().getLinks()).isNotNull();
    assertThat(workflow.getGraph().getLinks().size()).isEqualTo(2);
  }

  /**
   * Should trigger simple workflow.
   *
   * @throws InterruptedException
   */
  @Test
  public void shouldTriggerSimpleWorkflow() throws InterruptedException {
    Graph graph =
        aGraph()
            .addNodes(aNode().withId("n0").withName("ORIGIN").withX(200).withY(50).build())
            .addNodes(aNode()
                          .withId("n1")
                          .withName("RepeatByInstances")
                          .withX(200)
                          .withY(50)
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${instances()}")
                          .addProperty("executionStrategyExpression", "${SIMPLE_WORKFLOW_REPEAT_STRATEGY}")
                          .build())
            .addNodes(
                aNode()
                    .withId("n2")
                    .withName("email")
                    .withX(250)
                    .withY(50)
                    .withType(StateType.EMAIL.name())
                    .addProperty("toAddress", "a@b.com")
                    .addProperty("subject", "commandName : ${SIMPLE_WORKFLOW_COMMAND_NAME}")
                    .addProperty("body",
                        "service:${service.name}, serviceTemplate:${serviceTemplate.name}, host:${host.name}, instance:${instance.name}")
                    .build())
            .addLinks(aLink().withId("l0").withFrom("n0").withTo("n1").withType("success").build())
            .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("repeat").build())
            .build();

    when(staticConfiguration.defaultSimpleWorkflow()).thenReturn(graph);

    env = getEnvironment();

    Host host1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(appId).withInfraId(INFRA_ID).withHostName("host1").build());
    Host host2 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(appId).withInfraId(INFRA_ID).withHostName("host2").build());
    Service service = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(UUIDGenerator.getUuid()).withName("svc1").build());
    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(appId)
            .withEnvId(env.getUuid())
            .withService(service)
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());

    Builder builder = aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(appId).withEnvId(env.getUuid());

    String uuid1 = serviceInstanceService.save(builder.withHost(host1).build()).getUuid();
    String uuid2 = serviceInstanceService.save(builder.withHost(host2).build()).getUuid();

    ExecutionArgs executionArgs = new ExecutionArgs();
    List<String> serviceInstanceIds = new ArrayList<>();
    serviceInstanceIds.add(uuid1);
    serviceInstanceIds.add(uuid2);
    executionArgs.setServiceInstanceIds(serviceInstanceIds);
    executionArgs.setExecutionStrategy(ExecutionStrategy.SERIAL);
    executionArgs.setCommandName("START");
    executionArgs.setWorkflowType(WorkflowType.SIMPLE);
    executionArgs.setServiceId("123");

    WorkflowServiceImpl impl = (WorkflowServiceImpl) workflowService;

    impl.setStaticConfiguration(staticConfiguration);

    WorkflowExecution workflowExecution = impl.triggerEnvExecution(appId, env.getUuid(), executionArgs);
    assertThat(workflowExecution).isNotNull();
    assertThat(workflowExecution.getUuid()).isNotNull();

    WorkflowExecution workflowExecution2 = workflowService.getExecutionDetails(appId, workflowExecution.getUuid());
    if (workflowExecution2.getStatus() == ExecutionStatus.NEW
        || workflowExecution2.getStatus() == ExecutionStatus.RUNNING) {
      Thread.sleep(3000);
    }
    workflowExecution2 = workflowService.getExecutionDetails(appId, workflowExecution.getUuid());
    assertThat(workflowExecution2)
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getAppId, WorkflowExecution::getStateMachineId,
            WorkflowExecution::getWorkflowId)
        .containsExactly(workflowExecution.getUuid(), appId, workflowExecution.getStateMachineId(),
            workflowExecution.getWorkflowId());
    assertThat(workflowExecution2.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(workflowExecution2.getGraph()).isNotNull();
  }
}
