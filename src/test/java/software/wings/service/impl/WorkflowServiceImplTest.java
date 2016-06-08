/**
 *
 */
package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceInstance.ServiceInstanceBuilder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.RELEASE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Artifact.Builder;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentBuilder;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.Host.HostBuilder;
import software.wings.beans.Orchestration;
import software.wings.beans.Release.ReleaseBuilder;
import software.wings.beans.ServiceInstance.ServiceInstanceBuilder;
import software.wings.beans.WorkflowType;
import software.wings.common.UUIDGenerator;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * @author Rishi
 *
 */
public class WorkflowServiceImplTest extends WingsBaseTest {
  private static String appId = UUIDGenerator.getUuid();
  @Inject private WorkflowService workflowService;
  @Inject private WingsPersistence wingsPersistence;

  @Inject private ServiceInstanceService serviceInstanceService;

  private Environment env;

  public Environment getEnvironment() {
    if (env == null) {
      env = wingsPersistence.saveAndGet(Environment.class, EnvironmentBuilder.anEnvironment().withAppId(appId).build());
    }
    return env;
  }

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

  @Test
  public void shouldTriggerSimpleWorkflow() {
    env = getEnvironment();

    ServiceInstanceBuilder builder =
        aServiceInstance()
            .withHost(HostBuilder.aHost().withUuid(HOST_ID).build())
            .withServiceTemplate(
                aServiceTemplate().withUuid(TEMPLATE_ID).withService(aService().withUuid(SERVICE_ID).build()).build())
            .withRelease(ReleaseBuilder.aRelease().withUuid(RELEASE_ID).build())
            .withArtifact(Builder.anArtifact().withUuid(ARTIFACT_ID).build())
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
    executionArgs.setExecutionStrategy(ExecutionStrategy.PARALLEL);
    impl.triggerSimpleExecution(appId, env.getUuid(), executionArgs);
  }
}
