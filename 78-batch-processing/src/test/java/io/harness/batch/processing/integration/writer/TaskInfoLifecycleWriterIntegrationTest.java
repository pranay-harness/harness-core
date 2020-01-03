package io.harness.batch.processing.integration.writer;

import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_START;
import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_STOP;
import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Timestamp;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
import io.harness.batch.processing.integration.EcsEventGenerator;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.category.element.UnitTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.grpc.utils.HTimestamps;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import lombok.val;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class TaskInfoLifecycleWriterIntegrationTest extends CategoryTest implements EcsEventGenerator {
  private final String LAUNCH_TYPE = "EC2";
  private final String TEST_TASK_ARN = "TASK_ARN_" + this.getClass().getSimpleName();
  private final String TEST_ACCOUNT_ID = "ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID = "INSTANCE_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ARN = "CLUSTER_ARN_" + this.getClass().getSimpleName();
  private final String TEST_SERVICE_NAME = "SERVICE_NAME_" + this.getClass().getSimpleName();
  private final String TEST_CONTAINER_ARN = "CONTAINER_ARN_" + this.getClass().getSimpleName();
  private final String TEST_CLOUD_PROVIDER_ID = "CLOUD_PROVIDER_" + this.getClass().getSimpleName();

  private final Instant NOW = Instant.now();
  private final Timestamp INSTANCE_STOP_TIMESTAMP = HTimestamps.fromInstant(NOW);
  private final Timestamp INSTANCE_START_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(1, ChronoUnit.DAYS));

  @Autowired @Qualifier("ecsTaskInfoWriter") private ItemWriter<PublishedMessage> ecsTaskInfoWriter;
  @Autowired @Qualifier("ecsTaskLifecycleWriter") private ItemWriter<PublishedMessage> ecsTaskLifecycleWriter;
  @Autowired @Qualifier("ec2InstanceInfoWriter") private ItemWriter<PublishedMessage> ec2InstanceInfoWriter;
  @Autowired
  @Qualifier("ecsContainerInstanceInfoWriter")
  private ItemWriter<PublishedMessage> ecsContainerInstanceInfoWriter;
  @Autowired private HPersistence hPersistence;
  @Autowired private InstanceDataService instanceDataService;

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateTaskData() throws Exception {
    PublishedMessage ec2InstanceInfoMessage =
        getEc2InstanceInfoMessage(TEST_INSTANCE_ID, TEST_ACCOUNT_ID, TEST_CLUSTER_ARN);
    ec2InstanceInfoWriter.write(getMessageList(ec2InstanceInfoMessage));

    PublishedMessage containerInstanceInfoMessage = getContainerInstanceInfoMessage(
        TEST_CONTAINER_ARN, TEST_INSTANCE_ID, TEST_CLOUD_PROVIDER_ID, TEST_CLUSTER_ARN, TEST_ACCOUNT_ID);
    ecsContainerInstanceInfoWriter.write(getMessageList(containerInstanceInfoMessage));

    PublishedMessage taskInfoMessage = getTaskInfoMessage(
        TEST_TASK_ARN, TEST_SERVICE_NAME, LAUNCH_TYPE, TEST_CONTAINER_ARN, TEST_CLUSTER_ARN, TEST_ACCOUNT_ID);
    ecsTaskInfoWriter.write(getMessageList(taskInfoMessage));

    List<InstanceState> activeInstanceState = getActiveInstanceState();
    InstanceData instanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_TASK_ARN, activeInstanceState);

    assertThat(instanceData.getInstanceState()).isEqualTo(InstanceState.INITIALIZING);
    assertThat(instanceData.getInstanceId()).isEqualTo(TEST_TASK_ARN);
    assertThat(instanceData.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateEc2InstanceLifecycle() throws Exception {
    shouldCreateTaskData();

    // start task
    PublishedMessage taskLifecycleStartMessage =
        getTaskLifecycleMessage(INSTANCE_START_TIMESTAMP, EVENT_TYPE_START, TEST_TASK_ARN, TEST_ACCOUNT_ID);
    ecsTaskLifecycleWriter.write(getMessageList(taskLifecycleStartMessage));

    List<InstanceState> activeInstanceState = getActiveInstanceState();
    InstanceData instanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_TASK_ARN, activeInstanceState);
    assertThat(instanceData.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(HTimestamps.fromInstant(instanceData.getUsageStartTime())).isEqualTo(INSTANCE_START_TIMESTAMP);

    // stop task
    PublishedMessage taskLifecycleStopMessage =
        getTaskLifecycleMessage(INSTANCE_STOP_TIMESTAMP, EVENT_TYPE_STOP, TEST_TASK_ARN, TEST_ACCOUNT_ID);
    ecsTaskLifecycleWriter.write(getMessageList(taskLifecycleStopMessage));

    List<InstanceState> stoppedInstanceState = getStoppedInstanceState();
    InstanceData stoppedTaskInstanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_TASK_ARN, stoppedInstanceState);

    assertThat(stoppedInstanceState).isNotNull();
    assertThat(HTimestamps.fromInstant(stoppedTaskInstanceData.getUsageStopTime())).isEqualTo(INSTANCE_STOP_TIMESTAMP);
    assertThat(stoppedTaskInstanceData.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(stoppedTaskInstanceData.getInstanceId()).isEqualTo(TEST_TASK_ARN);
    assertThat(stoppedTaskInstanceData.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(stoppedTaskInstanceData.getClusterName()).isEqualTo(TEST_CLUSTER_ARN);

    Map<String, String> metaData = stoppedTaskInstanceData.getMetaData();
    assertThat(metaData.get(InstanceMetaDataConstants.CONTAINER_INSTANCE_ARN)).isEqualTo(TEST_CONTAINER_ARN);
  }

  @After
  public void clearCollection() {
    val instanceDataDataSore = hPersistence.getDatastore(InstanceData.class);
    instanceDataDataSore.delete(
        instanceDataDataSore.createQuery(InstanceData.class).filter(InstanceDataKeys.accountId, TEST_ACCOUNT_ID));
  }
}
