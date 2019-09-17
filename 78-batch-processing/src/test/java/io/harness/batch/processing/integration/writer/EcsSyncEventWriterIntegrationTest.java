package io.harness.batch.processing.integration.writer;

import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_START;
import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_STOP;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Timestamp;

import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
import io.harness.batch.processing.integration.EcsEventGenerator;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.category.element.IntegrationTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.grpc.utils.HTimestamps;
import io.harness.persistence.HPersistence;
import lombok.val;
import org.junit.After;
import org.junit.Before;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class EcsSyncEventWriterIntegrationTest implements EcsEventGenerator {
  private final String TEST_ACCOUNT_ID = "EC2_INSTANCE_INFO_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID = "EC2_INSTANCE_INFO_INSTANCE_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ARN = "EC2_INSTANCE_INFO_CLUSTER_ARN_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID_STOPPED =
      "EC2_INSTANCE_INFO_INSTANCE_ID_STOPPED_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID_RUNNING =
      "EC2_INSTANCE_INFO_INSTANCE_ID_RUNNING_" + this.getClass().getSimpleName();
  private final String TEST_ACTIVE_INSTANCE_ID =
      "EC2_INSTANCE_INFO_ACTIVE_INSTANCE_ID_" + this.getClass().getSimpleName();

  @Autowired @Qualifier("ec2InstanceInfoWriter") private ItemWriter<PublishedMessage> ec2InstanceInfoWriter;

  @Autowired @Qualifier("ec2InstanceLifecycleWriter") private ItemWriter<PublishedMessage> ec2InstanceLifecycleWriter;

  @Autowired @Qualifier("ecsSyncEventWriter") private ItemWriter<PublishedMessage> ecsSyncEventWriter;

  @Autowired private InstanceDataService instanceDataService;

  @Autowired private HPersistence hPersistence;

  private final Instant NOW = Instant.now();
  private final Timestamp RUNNING_INSTANCE_START_TIMESTAMP = HTimestamps.fromInstant(NOW);
  private final Timestamp INSTANCE_STOP_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(1, ChronoUnit.DAYS));
  private final Timestamp INSTANCE_START_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(2, ChronoUnit.DAYS));

  @Before
  public void setUpData() throws Exception {
    PublishedMessage ec2InstanceInfoMessage =
        getEc2InstanceInfoMessage(TEST_INSTANCE_ID_STOPPED, TEST_ACCOUNT_ID, TEST_CLUSTER_ARN);
    ec2InstanceInfoWriter.write(getMessageList(ec2InstanceInfoMessage));

    PublishedMessage ec2InstanceLifecycleStartMessage = getEc2InstanceLifecycleMessage(
        INSTANCE_START_TIMESTAMP, EVENT_TYPE_START, TEST_INSTANCE_ID_STOPPED, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleWriter.write(getMessageList(ec2InstanceLifecycleStartMessage));

    PublishedMessage ec2InstanceLifecycleStopMessage = getEc2InstanceLifecycleMessage(
        INSTANCE_STOP_TIMESTAMP, EVENT_TYPE_STOP, TEST_INSTANCE_ID_STOPPED, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleWriter.write(getMessageList(ec2InstanceLifecycleStopMessage));

    PublishedMessage runningEc2InstanceInfoMessage =
        getEc2InstanceInfoMessage(TEST_INSTANCE_ID_RUNNING, TEST_ACCOUNT_ID, TEST_CLUSTER_ARN);
    ec2InstanceInfoWriter.write(getMessageList(runningEc2InstanceInfoMessage));

    PublishedMessage runningEc2InstanceLifecycleStartMessage = getEc2InstanceLifecycleMessage(
        RUNNING_INSTANCE_START_TIMESTAMP, EVENT_TYPE_START, TEST_INSTANCE_ID_RUNNING, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleWriter.write(getMessageList(runningEc2InstanceLifecycleStartMessage));
  }

  @Test
  @Category(IntegrationTests.class)
  public void shouldStopEc2Instance() throws Exception {
    PublishedMessage ec2InstanceInfoMessage =
        getEc2InstanceInfoMessage(TEST_INSTANCE_ID, TEST_ACCOUNT_ID, TEST_CLUSTER_ARN);
    ec2InstanceInfoWriter.write(getMessageList(ec2InstanceInfoMessage));

    PublishedMessage ec2InstanceLifecycleStartMessage =
        getEc2InstanceLifecycleMessage(INSTANCE_START_TIMESTAMP, EVENT_TYPE_START, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleWriter.write(getMessageList(ec2InstanceLifecycleStartMessage));

    List<String> activeEc2InstanceList = new ArrayList<>(Arrays.asList(TEST_ACTIVE_INSTANCE_ID));
    PublishedMessage ecsSyncEventMessage = getEcsSyncEventMessage(TEST_ACCOUNT_ID, TEST_CLUSTER_ARN,
        Collections.emptyList(), activeEc2InstanceList, Collections.emptyList(), INSTANCE_STOP_TIMESTAMP);
    ecsSyncEventWriter.write(getMessageList(ecsSyncEventMessage));

    List<InstanceState> stoppedInstanceState = getStoppedInstanceState();
    InstanceData stoppedInstanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, stoppedInstanceState);
    assertThat(stoppedInstanceData).isNotNull();
    assertThat(HTimestamps.fromInstant(stoppedInstanceData.getUsageStartTime())).isEqualTo(INSTANCE_START_TIMESTAMP);
    assertThat(HTimestamps.fromInstant(stoppedInstanceData.getUsageStopTime())).isEqualTo(INSTANCE_STOP_TIMESTAMP);

    List<InstanceState> activeInstanceState = getActiveInstanceState();
    InstanceData activeInstanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID_RUNNING, activeInstanceState);
    assertThat(activeInstanceData).isNotNull();
  }

  @After
  public void clearCollection() {
    val instanceDataStore = hPersistence.getDatastore(InstanceData.class);
    instanceDataStore.delete(
        instanceDataStore.createQuery(InstanceData.class).filter(InstanceDataKeys.accountId, TEST_ACCOUNT_ID));
  }
}
