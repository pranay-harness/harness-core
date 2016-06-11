/**
 *
 */

package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Host.HostBuilder.aHost;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_ID;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentBuilder;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.Host;
import software.wings.beans.Orchestration;
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
   * @throws InterruptedException
   */
  @Test
  public void shouldTriggerSimpleWorkflow() throws InterruptedException {
    env = getEnvironment();

    Host host = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(appId).withInfraId(INFRA_ID).withHostName(HOST_NAME).build());
    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(appId)
            .withEnvId(env.getUuid())
            .withService(aService().withUuid("SERVICE_ID").build())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());

    Builder builder = aServiceInstance()
                          .withHost(host)
                          .withServiceTemplate(serviceTemplate)
                          .withAppId(appId)
                          .withEnvId(env.getUuid());

    String uuid1 = serviceInstanceService.save(builder.build()).getUuid();
    String uuid2 = serviceInstanceService.save(builder.build()).getUuid();
    WorkflowServiceImpl impl = (WorkflowServiceImpl) workflowService;

    ExecutionArgs executionArgs = new ExecutionArgs();
    List<String> serviceInstanceIds = new ArrayList<>();
    serviceInstanceIds.add(uuid1);
    serviceInstanceIds.add(uuid2);
    executionArgs.setServiceInstanceIds(serviceInstanceIds);
    executionArgs.setExecutionStrategy(ExecutionStrategy.SERIAL);
    WorkflowExecution workflowExecution = impl.triggerSimpleExecution(appId, env.getUuid(), executionArgs);
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
