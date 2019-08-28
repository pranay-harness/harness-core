package io.harness.batch.processing.integration.writer;

import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_START;
import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_STOP;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
import io.harness.batch.processing.integration.EcsEventGenerator;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.category.element.IntegrationTests;
import io.harness.event.grpc.PublishedMessage;
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
import java.util.Collections;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class Ec2InstanceInfoLifecycleWriterIntegrationTest implements EcsEventGenerator {
  private final String TEST_ACCOUNT_ID = "EC2_INSTANCE_INFO_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID = "EC2_INSTANCE_INFO_INSTANCE_ID_" + this.getClass().getSimpleName();

  @Autowired @Qualifier("ec2InstanceInfoWriter") private ItemWriter<PublishedMessage> ec2InstanceInfoWriter;

  @Autowired @Qualifier("ec2InstanceLifecycleWriter") private ItemWriter<PublishedMessage> ec2InstanceLifecycleWriter;

  @Autowired private InstanceDataService instanceDataService;

  @Autowired private HPersistence hPersistence;

  @Test
  @Category(IntegrationTests.class)
  public void shouldCreateEc2InstanceData() throws Exception {
    PublishedMessage ec2InstanceInfoMessage = getEc2InstanceInfoMessage(TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceInfoWriter.write(getMessageList(ec2InstanceInfoMessage));

    List<InstanceState> activeInstanceState = getActiveInstanceState();
    InstanceData instanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, activeInstanceState);

    assertThat(instanceData.getInstanceState()).isEqualTo(InstanceState.INITIALIZING);
    assertThat(instanceData.getInstanceId()).isEqualTo(TEST_INSTANCE_ID);
    assertThat(instanceData.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
  }

  @Test
  @Category(IntegrationTests.class)
  public void shouldCreateEc2InstanceLifecycle() throws Exception {
    shouldCreateEc2InstanceData();

    // start instance
    Timestamp startTimestamp = Timestamps.fromMillis(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
    PublishedMessage ec2InstanceLifecycleStartMessage =
        getEc2InstanceLifecycleMessage(startTimestamp, EVENT_TYPE_START, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleWriter.write(getMessageList(ec2InstanceLifecycleStartMessage));

    List<InstanceState> activeInstanceState = getActiveInstanceState();
    InstanceData instanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, activeInstanceState);
    assertThat(instanceData.getInstanceState()).isEqualTo(InstanceState.RUNNING);
    assertThat(instanceData.getUsageStartTime().toEpochMilli()).isEqualTo(startTimestamp.getSeconds() * 1000);

    // stop instance
    Timestamp endTimestamp = Timestamps.fromMillis(System.currentTimeMillis());
    PublishedMessage ec2InstanceLifecycleStopMessage =
        getEc2InstanceLifecycleMessage(endTimestamp, EVENT_TYPE_STOP, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleWriter.write(getMessageList(ec2InstanceLifecycleStopMessage));

    List<InstanceState> stoppedInstanceState = getStoppedInstanceState();
    InstanceData stoppedInstanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, stoppedInstanceState);

    assertThat(stoppedInstanceData.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(stoppedInstanceData.getInstanceId()).isEqualTo(TEST_INSTANCE_ID);
    assertThat(stoppedInstanceData.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(stoppedInstanceData.getUsageStopTime().toEpochMilli()).isEqualTo(endTimestamp.getSeconds() * 1000);
  }

  private List<PublishedMessage> getMessageList(PublishedMessage publishedMessage) {
    return Collections.singletonList(publishedMessage);
  }

  private List<InstanceState> getActiveInstanceState() {
    return new ArrayList<>(Arrays.asList(InstanceState.INITIALIZING, InstanceState.RUNNING));
  }

  private List<InstanceState> getStoppedInstanceState() {
    return Collections.singletonList(InstanceState.STOPPED);
  }

  @After
  public void clearCollection() {
    val ds = hPersistence.getDatastore(InstanceData.class);
    ds.delete(ds.createQuery(InstanceData.class).filter(InstanceDataKeys.accountId, TEST_ACCOUNT_ID));
  }
}
