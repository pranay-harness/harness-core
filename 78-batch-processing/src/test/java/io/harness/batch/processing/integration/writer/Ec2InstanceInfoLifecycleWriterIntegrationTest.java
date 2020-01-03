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
public class Ec2InstanceInfoLifecycleWriterIntegrationTest extends CategoryTest implements EcsEventGenerator {
  private final String TEST_ACCOUNT_ID = "EC2_INSTANCE_INFO_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ARN = "EC2_INSTANCE_INFO_CLUSTER_ARN_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID = "EC2_INSTANCE_INFO_INSTANCE_ID_" + this.getClass().getSimpleName();

  @Autowired @Qualifier("ec2InstanceInfoWriter") private ItemWriter<PublishedMessage> ec2InstanceInfoWriter;

  @Autowired @Qualifier("ec2InstanceLifecycleWriter") private ItemWriter<PublishedMessage> ec2InstanceLifecycleWriter;

  @Autowired private InstanceDataService instanceDataService;

  @Autowired private HPersistence hPersistence;

  private final Instant NOW = Instant.now();
  private final Timestamp INSTANCE_START_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(4, ChronoUnit.DAYS));
  private final Timestamp INSTANCE_STOP_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(3, ChronoUnit.DAYS));
  private final Timestamp INSTANCE_NEXT_START_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(2, ChronoUnit.DAYS));
  private final Timestamp INSTANCE_NEXT_STOP_TIMESTAMP = HTimestamps.fromInstant(NOW);

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateEc2InstanceData() throws Exception {
    PublishedMessage ec2InstanceInfoMessage =
        getEc2InstanceInfoMessage(TEST_INSTANCE_ID, TEST_ACCOUNT_ID, TEST_CLUSTER_ARN);
    ec2InstanceInfoWriter.write(getMessageList(ec2InstanceInfoMessage));

    List<InstanceState> activeInstanceState = getActiveInstanceState();
    InstanceData instanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, activeInstanceState);

    assertThat(instanceData.getInstanceState()).isEqualTo(InstanceState.INITIALIZING);
    assertThat(instanceData.getInstanceId()).isEqualTo(TEST_INSTANCE_ID);
    assertThat(instanceData.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateEc2InstanceLifecycle() throws Exception {
    PublishedMessage ec2InstanceInfoMessage =
        getEc2InstanceInfoMessage(TEST_INSTANCE_ID, TEST_ACCOUNT_ID, TEST_CLUSTER_ARN);
    ec2InstanceInfoWriter.write(getMessageList(ec2InstanceInfoMessage));

    // start instance
    PublishedMessage ec2InstanceLifecycleStartMessage =
        getEc2InstanceLifecycleMessage(INSTANCE_START_TIMESTAMP, EVENT_TYPE_START, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleWriter.write(getMessageList(ec2InstanceLifecycleStartMessage));

    List<InstanceState> activeInstanceState = getActiveInstanceState();
    InstanceData instanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, activeInstanceState);
    assertThat(instanceData.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(HTimestamps.fromInstant(instanceData.getUsageStartTime())).isEqualTo(INSTANCE_START_TIMESTAMP);

    // stop instance
    PublishedMessage ec2InstanceLifecycleStopMessage =
        getEc2InstanceLifecycleMessage(INSTANCE_STOP_TIMESTAMP, EVENT_TYPE_STOP, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleWriter.write(getMessageList(ec2InstanceLifecycleStopMessage));

    List<InstanceState> stoppedInstanceState = getStoppedInstanceState();
    InstanceData stoppedInstanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, stoppedInstanceState);

    assertThat(stoppedInstanceData.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(stoppedInstanceData.getInstanceId()).isEqualTo(TEST_INSTANCE_ID);
    assertThat(stoppedInstanceData.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(HTimestamps.fromInstant(stoppedInstanceData.getUsageStopTime())).isEqualTo(INSTANCE_STOP_TIMESTAMP);

    ec2InstanceInfoWriter.write(getMessageList(ec2InstanceInfoMessage));

    PublishedMessage ec2InstanceLifecycleNextStartMessage = getEc2InstanceLifecycleMessage(
        INSTANCE_NEXT_START_TIMESTAMP, EVENT_TYPE_START, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleWriter.write(getMessageList(ec2InstanceLifecycleNextStartMessage));

    InstanceData nextInstanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, activeInstanceState);
    assertThat(nextInstanceData.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(HTimestamps.fromInstant(nextInstanceData.getUsageStartTime())).isEqualTo(INSTANCE_NEXT_START_TIMESTAMP);
  }

  @After
  public void clearCollection() {
    val instanceDs = hPersistence.getDatastore(InstanceData.class);
    instanceDs.delete(instanceDs.createQuery(InstanceData.class).filter(InstanceDataKeys.accountId, TEST_ACCOUNT_ID));
  }
}
