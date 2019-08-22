package io.harness.batch.processing.integration.writer;

import static org.junit.Assert.assertEquals;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
import io.harness.batch.processing.integration.EcsEventGenerator;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.category.element.IntegrationTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.Lifecycle.EventType;
import io.harness.persistence.HPersistence;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class Ec2InstanceInfoLifecycleWriterIntegrationTest implements EcsEventGenerator {
  private final String TEST_ACCOUNT_ID = "EC2_INSTANCE_INFO_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID = "EC2_INSTANCE_INFO_INSTANCE_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ARN = "EC2_INSTANCE_INFO_CLUSTER_ARN_" + this.getClass().getSimpleName();

  @Autowired @Qualifier("ec2InstanceInfoWriter") private ItemWriter ec2InstanceInfoWriter;

  @Autowired @Qualifier("ec2InstanceLifecycleWriter") private ItemWriter ec2InstanceLifecycleWriter;

  @Autowired private InstanceDataService instanceDataService;

  @Autowired private HPersistence hPersistence;

  @Test
  @Category(IntegrationTests.class)
  public void shouldCreateEc2InstanceData() throws Exception {
    PublishedMessage ec2InstanceInfoMessage = getEc2InstanceInfoMessage(TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceInfoWriter.write(getMessageList(ec2InstanceInfoMessage));

    List<io.harness.batch.processing.ccm.InstanceState> activeInstanceState = getActiveInstanceState();
    InstanceData instanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, activeInstanceState);

    assertEquals(io.harness.batch.processing.ccm.InstanceState.INITIALIZING, instanceData.getInstanceState());
    assertEquals(TEST_INSTANCE_ID, instanceData.getInstanceId());
    assertEquals(TEST_ACCOUNT_ID, instanceData.getAccountId());
  }

  @Test
  @Category(IntegrationTests.class)
  public void shouldCreateEc2InstanceLifecycle() throws Exception {
    shouldCreateEc2InstanceData();

    // start instance
    Timestamp startTimestamp = Timestamps.fromMillis(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
    PublishedMessage ec2InstanceLifecycleStartMessage =
        getEc2InstanceLifecycleMessage(startTimestamp, EventType.START, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleWriter.write(getMessageList(ec2InstanceLifecycleStartMessage));

    List<io.harness.batch.processing.ccm.InstanceState> activeInstanceState = getActiveInstanceState();
    InstanceData instanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, activeInstanceState);
    assertEquals(io.harness.batch.processing.ccm.InstanceState.RUNNING, instanceData.getInstanceState());
    assertEquals(startTimestamp.getSeconds() * 1000, instanceData.getUsageStartTime().toEpochMilli());

    // stop instance
    Timestamp endTimestamp = Timestamps.fromMillis(System.currentTimeMillis());
    PublishedMessage ec2InstanceLifecycleStopMessage =
        getEc2InstanceLifecycleMessage(endTimestamp, EventType.STOP, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleWriter.write(getMessageList(ec2InstanceLifecycleStopMessage));

    List<io.harness.batch.processing.ccm.InstanceState> stoppedInstanceState = getStoppedInstanceState();
    InstanceData stoppedInstanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, stoppedInstanceState);

    assertEquals(io.harness.batch.processing.ccm.InstanceState.STOPPED, stoppedInstanceData.getInstanceState());
    assertEquals(TEST_INSTANCE_ID, stoppedInstanceData.getInstanceId());
    assertEquals(TEST_ACCOUNT_ID, stoppedInstanceData.getAccountId());
    assertEquals(endTimestamp.getSeconds() * 1000, stoppedInstanceData.getUsageStopTime().toEpochMilli());
  }

  private List<PublishedMessage> getMessageList(PublishedMessage publishedMessage) {
    return new ArrayList(Arrays.asList(publishedMessage));
  }

  private List<io.harness.batch.processing.ccm.InstanceState> getActiveInstanceState() {
    return new ArrayList<>(Arrays.asList(io.harness.batch.processing.ccm.InstanceState.INITIALIZING,
        io.harness.batch.processing.ccm.InstanceState.RUNNING));
  }

  private List<io.harness.batch.processing.ccm.InstanceState> getStoppedInstanceState() {
    return new ArrayList<>(Arrays.asList(io.harness.batch.processing.ccm.InstanceState.STOPPED));
  }

  @After
  public void clearCollection() {
    val ds = hPersistence.getDatastore(InstanceData.class);
    ds.delete(ds.createQuery(InstanceData.class).filter(InstanceDataKeys.accountId, TEST_ACCOUNT_ID));
  }
}
