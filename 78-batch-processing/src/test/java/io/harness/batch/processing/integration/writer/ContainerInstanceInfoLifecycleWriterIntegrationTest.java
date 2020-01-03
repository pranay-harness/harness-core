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

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class ContainerInstanceInfoLifecycleWriterIntegrationTest extends CategoryTest implements EcsEventGenerator {
  private final String TEST_ACCOUNT_ID = "ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID = "INSTANCE_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ARN = "CLUSTER_ARN_" + this.getClass().getSimpleName();
  private final String TEST_CONTAINER_ARN = "CONTAINER_ARN_" + this.getClass().getSimpleName();
  private final String TEST_CLOUD_PROVIDER_ID = "CLOUD_PROVIDER_" + this.getClass().getSimpleName();

  @Autowired @Qualifier("ec2InstanceInfoWriter") private ItemWriter<PublishedMessage> ec2InstanceInfoWriter;
  @Autowired
  @Qualifier("ecsContainerInstanceInfoWriter")
  private ItemWriter<PublishedMessage> ecsContainerInstanceInfoWriter;
  @Autowired
  @Qualifier("ecsContainerInstanceLifecycleWriter")
  private ItemWriter<PublishedMessage> ecsContainerInstanceLifecycleWriter;

  @Autowired private HPersistence hPersistence;
  @Autowired private InstanceDataService instanceDataService;

  private final Instant NOW = Instant.now();
  private final Timestamp INSTANCE_STOP_TIMESTAMP = HTimestamps.fromInstant(NOW);
  private final Timestamp INSTANCE_START_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(1, ChronoUnit.DAYS));

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateContainerInstanceData() throws Exception {
    PublishedMessage ec2InstanceInfoMessage =
        getEc2InstanceInfoMessage(TEST_INSTANCE_ID, TEST_ACCOUNT_ID, TEST_CLUSTER_ARN);
    ec2InstanceInfoWriter.write(getMessageList(ec2InstanceInfoMessage));

    PublishedMessage containerInstanceInfoMessage = getContainerInstanceInfoMessage(
        TEST_CONTAINER_ARN, TEST_INSTANCE_ID, TEST_CLOUD_PROVIDER_ID, TEST_CLUSTER_ARN, TEST_ACCOUNT_ID);
    ecsContainerInstanceInfoWriter.write(getMessageList(containerInstanceInfoMessage));

    List<InstanceState> activeInstanceState = getActiveInstanceState();
    InstanceData instanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_CONTAINER_ARN, activeInstanceState);

    assertThat(instanceData.getInstanceState()).isEqualTo(InstanceState.INITIALIZING);
    assertThat(instanceData.getInstanceId()).isEqualTo(TEST_CONTAINER_ARN);
    assertThat(instanceData.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateEc2InstanceLifecycle() throws Exception {
    shouldCreateContainerInstanceData();

    // start container instance
    PublishedMessage ecsLifecycleStartMessage = getContainerInstanceLifecycleMessage(
        INSTANCE_START_TIMESTAMP, EVENT_TYPE_START, TEST_CONTAINER_ARN, TEST_ACCOUNT_ID);
    ecsContainerInstanceLifecycleWriter.write(getMessageList(ecsLifecycleStartMessage));

    List<InstanceState> activeInstanceState = getActiveInstanceState();
    InstanceData instanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_CONTAINER_ARN, activeInstanceState);
    assertThat(instanceData.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(HTimestamps.fromInstant(instanceData.getUsageStartTime())).isEqualTo(INSTANCE_START_TIMESTAMP);

    // stop container instance
    PublishedMessage ecsLifecycleStopMessage = getContainerInstanceLifecycleMessage(
        INSTANCE_STOP_TIMESTAMP, EVENT_TYPE_STOP, TEST_CONTAINER_ARN, TEST_ACCOUNT_ID);
    ecsContainerInstanceLifecycleWriter.write(getMessageList(ecsLifecycleStopMessage));

    List<InstanceState> stoppedInstanceState = getStoppedInstanceState();
    InstanceData stoppedContainerInstanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_CONTAINER_ARN, stoppedInstanceState);

    assertThat(HTimestamps.fromInstant(stoppedContainerInstanceData.getUsageStopTime()))
        .isEqualTo(INSTANCE_STOP_TIMESTAMP);
    assertThat(stoppedContainerInstanceData.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(stoppedContainerInstanceData.getInstanceId()).isEqualTo(TEST_CONTAINER_ARN);
    assertThat(stoppedContainerInstanceData.getInstanceState()).isEqualTo(InstanceState.STOPPED);
  }

  @After
  public void clearCollection() {
    val instanceDataDs = hPersistence.getDatastore(InstanceData.class);
    instanceDataDs.delete(
        instanceDataDs.createQuery(InstanceData.class).filter(InstanceDataKeys.accountId, TEST_ACCOUNT_ID));
  }
}
