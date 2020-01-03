package io.harness.batch.processing.writer;

import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_START;
import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_STOP;
import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Timestamp;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.integration.EcsEventGenerator;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.category.element.UnitTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.grpc.utils.HTimestamps;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class Ec2InstanceInfoLifecycleWriterTest extends CategoryTest implements EcsEventGenerator {
  @InjectMocks private Ec2InstanceInfoWriter ec2InstanceInfoWriter;
  @InjectMocks private Ec2InstanceLifecycleWriter ec2InstanceLifecycleWriter;
  @Mock private InstanceDataService instanceDataService;

  private final String TEST_ACCOUNT_ID = "EC2_INSTANCE_INFO_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID = "EC2_INSTANCE_INFO_INSTANCE_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ARN = "EC2_INSTANCE_INFO_CLUSTER_ARN_" + this.getClass().getSimpleName();

  private final Instant NOW = Instant.now();
  private final Timestamp INSTANCE_STOP_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(1, ChronoUnit.DAYS));
  private final Timestamp INSTANCE_START_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(2, ChronoUnit.DAYS));

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldWriteEc2InstanceInfo() throws Exception {
    PublishedMessage ec2InstanceInfoMessage =
        getEc2InstanceInfoMessage(TEST_INSTANCE_ID, TEST_ACCOUNT_ID, TEST_CLUSTER_ARN);
    ec2InstanceInfoWriter.write(Arrays.asList(ec2InstanceInfoMessage));
    ArgumentCaptor<InstanceData> instanceDataArgumentCaptor = ArgumentCaptor.forClass(InstanceData.class);
    verify(instanceDataService, times(1)).create(instanceDataArgumentCaptor.capture());
    InstanceData instanceData = instanceDataArgumentCaptor.getValue();
    assertThat(instanceData.getInstanceId()).isEqualTo(TEST_INSTANCE_ID);
    assertThat(instanceData.getInstanceType()).isEqualTo(InstanceType.EC2_INSTANCE);
    Map<String, String> instanceDataMetaData = instanceData.getMetaData();
    assertThat(instanceDataMetaData.get(InstanceMetaDataConstants.INSTANCE_FAMILY)).isNotNull();
    assertThat(instanceDataMetaData.get(InstanceMetaDataConstants.REGION)).isNotNull();
    assertThat(instanceData.getInstanceState()).isEqualTo(InstanceState.INITIALIZING);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void updateEc2InstantStartTime() throws Exception {
    when(instanceDataService.fetchActiveInstanceData(
             TEST_ACCOUNT_ID, TEST_INSTANCE_ID, Arrays.asList(InstanceState.INITIALIZING)))
        .thenReturn(createEc2InstanceData(TEST_INSTANCE_ID, TEST_ACCOUNT_ID, InstanceState.RUNNING));
    PublishedMessage ec2InstanceLifecycleMessage =
        getEc2InstanceLifecycleMessage(INSTANCE_START_TIMESTAMP, EVENT_TYPE_START, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleWriter.write(Arrays.asList(ec2InstanceLifecycleMessage));
    ArgumentCaptor<InstanceData> instanceDataArgumentCaptor = ArgumentCaptor.forClass(InstanceData.class);
    ArgumentCaptor<InstanceState> instanceStateArgumentCaptor = ArgumentCaptor.forClass(InstanceState.class);
    ArgumentCaptor<Instant> startTimeArgumentCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(instanceDataService, times(1))
        .updateInstanceState(instanceDataArgumentCaptor.capture(), startTimeArgumentCaptor.capture(),
            instanceStateArgumentCaptor.capture());
    assertThat(instanceStateArgumentCaptor.getValue()).isEqualTo(InstanceState.RUNNING);
    assertThat(startTimeArgumentCaptor.getValue()).isEqualTo(HTimestamps.toInstant(INSTANCE_START_TIMESTAMP));
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void updateEc2InstantStopTime() throws Exception {
    when(instanceDataService.fetchActiveInstanceData(
             TEST_ACCOUNT_ID, TEST_INSTANCE_ID, Arrays.asList(InstanceState.RUNNING)))
        .thenReturn(createEc2InstanceData(TEST_INSTANCE_ID, TEST_ACCOUNT_ID, InstanceState.RUNNING));
    PublishedMessage ec2InstanceStopLifecycleMessage =
        getEc2InstanceLifecycleMessage(INSTANCE_STOP_TIMESTAMP, EVENT_TYPE_STOP, TEST_INSTANCE_ID, TEST_ACCOUNT_ID);
    ec2InstanceLifecycleWriter.write(Arrays.asList(ec2InstanceStopLifecycleMessage));
    ArgumentCaptor<InstanceData> instanceDataArgumentCaptor = ArgumentCaptor.forClass(InstanceData.class);
    ArgumentCaptor<Instant> stopTimeArgumentCaptor = ArgumentCaptor.forClass(Instant.class);
    ArgumentCaptor<InstanceState> instanceStateArgumentCaptor = ArgumentCaptor.forClass(InstanceState.class);
    verify(instanceDataService, times(1))
        .updateInstanceState(instanceDataArgumentCaptor.capture(), stopTimeArgumentCaptor.capture(),
            instanceStateArgumentCaptor.capture());
    assertThat(stopTimeArgumentCaptor.getValue()).isEqualTo(HTimestamps.toInstant(INSTANCE_STOP_TIMESTAMP));
    assertThat(instanceStateArgumentCaptor.getValue()).isEqualTo(InstanceState.STOPPED);
  }
}
