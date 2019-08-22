package io.harness.batch.processing.integration.schedule;

import static org.junit.Assert.assertEquals;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.entities.BatchJobScheduledData;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
import io.harness.batch.processing.integration.EcsEventGenerator;
import io.harness.batch.processing.schedule.BatchJobRunner;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.category.element.IntegrationTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.grpc.PublishedMessage.PublishedMessageKeys;
import io.harness.event.payloads.Lifecycle.EventType;
import io.harness.persistence.HPersistence;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import software.wings.integration.BaseIntegrationTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class EcsJobIntegrationTest extends BaseIntegrationTest implements EcsEventGenerator {
  private final String TEST_ACCOUNT_ID = "EC2_INSTANCE_INFO_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID = "EC2_INSTANCE_INFO_INSTANCE_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ARN = "EC2_INSTANCE_INFO_CLUSTER_ARN_" + this.getClass().getSimpleName();
  private final Timestamp INSTANCE_START_TIMESTAMP =
      Timestamps.fromMillis(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
  private final Timestamp INSTANCE_STOP_TIMESTAMP = Timestamps.fromMillis(System.currentTimeMillis());

  @Autowired private InstanceDataService instanceDataService;

  @Autowired private HPersistence hPersistence;

  @Autowired private BatchJobRunner batchJobRunner;

  @Qualifier("ecsJob") @Autowired private Job ecsJob;

  @Before
  public void setUpData() {
    val batchJobDataDs = hPersistence.getDatastore(BatchJobScheduledData.class);
    batchJobDataDs.delete(batchJobDataDs.createQuery(BatchJobScheduledData.class));

    Instant createdTimestamp = Instant.now().minus(28, ChronoUnit.HOURS);
    PublishedMessage ec2InstanceInfoMessage = getEc2InstanceInfoMessage(TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceInfoMessage.setCreatedAt(createdTimestamp.toEpochMilli());
    hPersistence.save(ec2InstanceInfoMessage);

    PublishedMessage ec2InstanceInfoDuplicateMessage = getEc2InstanceInfoMessage(TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceInfoDuplicateMessage.setCreatedAt(createdTimestamp.plus(1, ChronoUnit.HOURS).toEpochMilli());
    hPersistence.save(ec2InstanceInfoDuplicateMessage);

    PublishedMessage ec2InstanceLifecycleStartMessage =
        getEc2InstanceLifecycleMessage(INSTANCE_START_TIMESTAMP, EventType.START, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleStartMessage.setCreatedAt(createdTimestamp.toEpochMilli());
    hPersistence.save(ec2InstanceLifecycleStartMessage);

    PublishedMessage ec2InstanceLifecycleStartDuplicateMessage =
        getEc2InstanceLifecycleMessage(INSTANCE_START_TIMESTAMP, EventType.START, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleStartDuplicateMessage.setCreatedAt(
        createdTimestamp.plus(30, ChronoUnit.MINUTES).toEpochMilli());
    hPersistence.save(ec2InstanceLifecycleStartDuplicateMessage);

    PublishedMessage ec2InstanceLifecycleStopMessage =
        getEc2InstanceLifecycleMessage(INSTANCE_STOP_TIMESTAMP, EventType.STOP, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleStopMessage.setCreatedAt(createdTimestamp.plus(45, ChronoUnit.MINUTES).toEpochMilli());
    hPersistence.save(ec2InstanceLifecycleStopMessage);

    PublishedMessage ec2InstanceLifecycleStartDuplicateTwoMessage =
        getEc2InstanceLifecycleMessage(INSTANCE_START_TIMESTAMP, EventType.START, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleStartDuplicateTwoMessage.setCreatedAt(
        createdTimestamp.plus(50, ChronoUnit.MINUTES).toEpochMilli());
    hPersistence.save(ec2InstanceLifecycleStartDuplicateTwoMessage);

    PublishedMessage ec2InstanceLifecycleStopDuplicateMessage =
        getEc2InstanceLifecycleMessage(INSTANCE_STOP_TIMESTAMP, EventType.STOP, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleStopDuplicateMessage.setCreatedAt(createdTimestamp.plus(1, ChronoUnit.HOURS).toEpochMilli());
    hPersistence.save(ec2InstanceLifecycleStopDuplicateMessage);
  }

  @Test
  @Category(IntegrationTests.class)
  public void shouldRunEcsJob() throws Exception {
    batchJobRunner.runJob(ecsJob, BatchJobType.ECS_EVENT, 1, ChronoUnit.DAYS);

    List<io.harness.batch.processing.ccm.InstanceState> stoppedInstanceState = getStoppedInstanceState();
    InstanceData stoppedInstanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, stoppedInstanceState);

    assertEquals(io.harness.batch.processing.ccm.InstanceState.STOPPED, stoppedInstanceData.getInstanceState());
    assertEquals(TEST_INSTANCE_ID, stoppedInstanceData.getInstanceId());
    assertEquals(TEST_ACCOUNT_ID, stoppedInstanceData.getAccountId());
    assertEquals(INSTANCE_START_TIMESTAMP.getSeconds() * 1000, stoppedInstanceData.getUsageStartTime().toEpochMilli());
    assertEquals(INSTANCE_STOP_TIMESTAMP.getSeconds() * 1000, stoppedInstanceData.getUsageStopTime().toEpochMilli());
  }

  private List<io.harness.batch.processing.ccm.InstanceState> getStoppedInstanceState() {
    return new ArrayList<>(Arrays.asList(io.harness.batch.processing.ccm.InstanceState.STOPPED));
  }

  @After
  public void clearCollection() {
    val batchJobDataDs = hPersistence.getDatastore(BatchJobScheduledData.class);
    batchJobDataDs.delete(batchJobDataDs.createQuery(BatchJobScheduledData.class));

    val ds = hPersistence.getDatastore(InstanceData.class);
    ds.delete(ds.createQuery(InstanceData.class).filter(InstanceDataKeys.accountId, TEST_ACCOUNT_ID));

    val publishedMessageDs = hPersistence.getDatastore(PublishedMessage.class);
    publishedMessageDs.delete(
        publishedMessageDs.createQuery(PublishedMessage.class).filter(PublishedMessageKeys.accountId, TEST_ACCOUNT_ID));
  }
}
