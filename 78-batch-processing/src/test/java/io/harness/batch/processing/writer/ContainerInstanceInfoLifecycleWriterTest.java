package io.harness.batch.processing.writer;

import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_START;
import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Timestamp;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.Resource;
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
public class ContainerInstanceInfoLifecycleWriterTest extends CategoryTest implements EcsEventGenerator {
  @InjectMocks private EcsContainerInstanceInfoWriter ecsContainerInstanceInfoWriter;
  @InjectMocks private EcsContainerInstanceLifecycleWriter ecsContainerInstanceLifecycleWriter;
  @Mock private InstanceDataService instanceDataService;

  private final String TEST_ACCOUNT_ID = "ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID = "INSTANCE_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ARN = "CLUSTER_ARN_" + this.getClass().getSimpleName();
  private final String TEST_CONTAINER_ARN = "CONTAINER_ARN_" + this.getClass().getSimpleName();
  private final String TEST_CLOUD_PROVIDER_ID = "CLOUD_PROVIDER_" + this.getClass().getSimpleName();

  private final Timestamp INSTANCE_START_TIMESTAMP = HTimestamps.fromInstant(Instant.now().minus(2, ChronoUnit.DAYS));

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldWriteContainerInstanceInfo() throws Exception {
    when(instanceDataService.fetchInstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID))
        .thenReturn(createEc2InstanceData(TEST_ACCOUNT_ID, TEST_INSTANCE_ID, InstanceState.RUNNING));
    PublishedMessage ec2InstanceInfoMessage = getContainerInstanceInfoMessage(
        TEST_CONTAINER_ARN, TEST_INSTANCE_ID, TEST_CLOUD_PROVIDER_ID, TEST_CLUSTER_ARN, TEST_ACCOUNT_ID);
    ecsContainerInstanceInfoWriter.write(Arrays.asList(ec2InstanceInfoMessage));
    ArgumentCaptor<InstanceData> instanceDataArgumentCaptor = ArgumentCaptor.forClass(InstanceData.class);
    verify(instanceDataService).create(instanceDataArgumentCaptor.capture());
    InstanceData instanceData = instanceDataArgumentCaptor.getValue();
    assertThat(instanceData.getInstanceId()).isEqualTo(TEST_CONTAINER_ARN);
    assertThat(instanceData.getInstanceType()).isEqualTo(InstanceType.ECS_CONTAINER_INSTANCE);
    Map<String, String> instanceDataMetaData = instanceData.getMetaData();
    assertThat(instanceDataMetaData.get(InstanceMetaDataConstants.INSTANCE_FAMILY)).isNotNull();
    assertThat(instanceDataMetaData.get(InstanceMetaDataConstants.REGION)).isNotNull();
    Resource totalResource = instanceData.getTotalResource();
    assertThat(totalResource).isNotNull();
    assertThat(totalResource.getCpuUnits()).isEqualTo(512);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void updateContainerInstantStartTime() throws Exception {
    when(instanceDataService.fetchActiveInstanceData(
             TEST_ACCOUNT_ID, TEST_CONTAINER_ARN, Arrays.asList(InstanceState.INITIALIZING)))
        .thenReturn(createContainerInstanceData(TEST_CONTAINER_ARN, TEST_ACCOUNT_ID, InstanceState.INITIALIZING));
    PublishedMessage containerInstanceLifecycleMessage = getContainerInstanceLifecycleMessage(
        INSTANCE_START_TIMESTAMP, EVENT_TYPE_START, TEST_CONTAINER_ARN, TEST_ACCOUNT_ID);
    ecsContainerInstanceLifecycleWriter.write(Arrays.asList(containerInstanceLifecycleMessage));
    ArgumentCaptor<InstanceData> containerInstanceDataArgumentCaptor = ArgumentCaptor.forClass(InstanceData.class);
    ArgumentCaptor<InstanceState> containerInstanceStateArgumentCaptor = ArgumentCaptor.forClass(InstanceState.class);
    ArgumentCaptor<Instant> containerInstanceStartTimeArgumentCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(instanceDataService)
        .updateInstanceState(containerInstanceDataArgumentCaptor.capture(),
            containerInstanceStartTimeArgumentCaptor.capture(), containerInstanceStateArgumentCaptor.capture());
    assertThat(containerInstanceStateArgumentCaptor.getValue()).isEqualTo(InstanceState.RUNNING);
    assertThat(containerInstanceStartTimeArgumentCaptor.getValue())
        .isEqualTo(HTimestamps.toInstant(INSTANCE_START_TIMESTAMP));
  }
}
