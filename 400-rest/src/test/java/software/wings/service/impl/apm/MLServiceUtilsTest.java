package software.wings.service.impl.apm;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.deployment.InstanceDetails;
import io.harness.rule.Owner;
import io.harness.time.Timestamp;

import software.wings.WingsBaseTest;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.service.impl.datadog.DataDogSetupTestNodeData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MLServiceUtilsTest extends WingsBaseTest {
  @Inject private MLServiceUtils mlServiceUtils;
  @Inject private WingsPersistence wingsPersistence;
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void getHostName_withInstanceDetails() {
    String workflowId = generateUuid();
    String appId = generateUuid();

    WorkflowExecution workflowExecution =
        WorkflowExecution.builder().appId(appId).status(ExecutionStatus.SUCCESS).workflowId(workflowId).build();
    wingsPersistence.save(workflowExecution);
    StateExecutionInstance stateExecutionInstance = StateExecutionInstance.Builder.aStateExecutionInstance()
                                                        .displayName("context")
                                                        .executionUuid(workflowExecution.getUuid())
                                                        .stateType(StateType.PHASE.toString())
                                                        .status(ExecutionStatus.SUCCESS)
                                                        .build();
    wingsPersistence.save(stateExecutionInstance);
    DataDogSetupTestNodeData nodeData =
        DataDogSetupTestNodeData.builder()
            .workflowId(workflowExecution.getWorkflowId())
            .appId(appId)
            .deploymentType("KUBERNETES")
            .metrics("docker.mem.rss")
            .fromTime(Timestamp.currentMinuteBoundary())
            .instanceName("harness-example-prod-deployment-6cd9fc7f94-2f58p")
            .toTime(Timestamp.currentMinuteBoundary())
            .hostExpression("${instanceDetails.k8s.ip}")
            .instanceElement(SetupTestNodeData.Instance.builder()
                                 .instanceDetails(InstanceDetails.builder()
                                                      .hostName("harness-example-prod-deployment-6cd9fc7f94-2f58p")
                                                      .k8s(InstanceDetails.K8s.builder().ip("123.123.123.123").build())
                                                      .build())
                                 .build())
            .stateType(StateType.DATA_DOG)
            .guid(generateUuid())
            .build();
    assertThat(mlServiceUtils.getHostName(nodeData)).isEqualTo("123.123.123.123");
    nodeData.setHostExpression("${instanceDetails.hostName}");
    assertThat(mlServiceUtils.getHostName(nodeData)).isEqualTo("harness-example-prod-deployment-6cd9fc7f94-2f58p");
    nodeData.setHostExpression(null);
    assertThat(mlServiceUtils.getHostName(nodeData)).isEqualTo("harness-example-prod-deployment-6cd9fc7f94-2f58p");
  }
}
