package io.harness.batch.processing.writer;

import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_START;
import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
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
import io.harness.event.payloads.EcsTaskInfo;
import io.harness.exception.InvalidRequestException;
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
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class EcsTaskInfoLifecycleWriterTest extends CategoryTest implements EcsEventGenerator {
  @InjectMocks private EcsTaskInfoWriter ecsTaskInfoWriter;
  @InjectMocks private EcsTaskLifecycleWriter ecsTaskLifecycleWriter;
  @Mock private InstanceDataService instanceDataService;
  @Mock private CloudToHarnessMappingService cloudToHarnessMappingService;

  private final String LAUNCH_TYPE = "EC2";
  private final String FARGATE_LAUNCH_TYPE = "FARGATE";
  private final String UNKNOWN_LAUNCH_TYPE = "UNKNOWN";
  private final String TEST_TASK_ARN = "TASK_ARN_" + this.getClass().getSimpleName();
  private final String TEST_ACCOUNT_ID = "ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ID = "CLUSTER_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ARN = "CLUSTER_ARN_" + this.getClass().getSimpleName();
  private final String TEST_SERVICE_NAME = "SERVICE_NAME_" + this.getClass().getSimpleName();
  private final String TEST_CONTAINER_ARN = "CONTAINER_ARN_" + this.getClass().getSimpleName();

  private final Timestamp INSTANCE_START_TIMESTAMP = HTimestamps.fromInstant(Instant.now());

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldWriteTaskInfo() throws Exception {
    when(instanceDataService.fetchInstanceData(TEST_ACCOUNT_ID, TEST_CONTAINER_ARN))
        .thenReturn(createContainerInstanceData(TEST_ACCOUNT_ID, TEST_CONTAINER_ARN, InstanceState.RUNNING));
    when(cloudToHarnessMappingService.getHarnessServiceInfo(any())).thenReturn(createHarnessServiceInfo());
    PublishedMessage ec2InstanceInfoMessage = getTaskInfoMessage(TEST_TASK_ARN, TEST_SERVICE_NAME, LAUNCH_TYPE,
        TEST_CONTAINER_ARN, TEST_CLUSTER_ARN, TEST_ACCOUNT_ID, TEST_CLUSTER_ID);
    ecsTaskInfoWriter.write(Arrays.asList(ec2InstanceInfoMessage));
    ArgumentCaptor<InstanceData> instanceDataArgumentCaptor = ArgumentCaptor.forClass(InstanceData.class);
    verify(instanceDataService).create(instanceDataArgumentCaptor.capture());
    InstanceData instanceData = instanceDataArgumentCaptor.getValue();
    assertThat(instanceData.getInstanceId()).isEqualTo(TEST_TASK_ARN);
    assertThat(instanceData.getInstanceType()).isEqualTo(InstanceType.ECS_TASK_EC2);
    Map<String, String> instanceDataMetaData = instanceData.getMetaData();
    assertThat(instanceDataMetaData.get(InstanceMetaDataConstants.INSTANCE_FAMILY)).isNotNull();
    assertThat(instanceDataMetaData.get(InstanceMetaDataConstants.REGION)).isNotNull();
    assertThat(instanceDataMetaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_CPU))
        .isEqualTo(String.valueOf(512.0));
    assertThat(instanceDataMetaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_MEMORY))
        .isEqualTo(String.valueOf(1024.0));
    Resource totalResource = instanceData.getTotalResource();
    assertThat(totalResource).isNotNull();
    assertThat(totalResource.getCpuUnits()).isEqualTo(512);
    assertThat(instanceData.getHarnessServiceInfo()).isEqualTo(createHarnessServiceInfo().get());
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhenParentResourceNotPresent() throws Exception {
    when(instanceDataService.fetchInstanceData(TEST_ACCOUNT_ID, TEST_CONTAINER_ARN)).thenReturn(null);
    PublishedMessage ec2InstanceInfoMessage = getTaskInfoMessage(
        TEST_TASK_ARN, "", LAUNCH_TYPE, TEST_CONTAINER_ARN, TEST_CLUSTER_ARN, TEST_ACCOUNT_ID, TEST_CLUSTER_ID);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> ecsTaskInfoWriter.write(Arrays.asList(ec2InstanceInfoMessage)));
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void updateTaskStartTime() throws Exception {
    when(instanceDataService.fetchActiveInstanceData(
             TEST_ACCOUNT_ID, TEST_CLUSTER_ID, TEST_TASK_ARN, Arrays.asList(InstanceState.INITIALIZING)))
        .thenReturn(createTaskInstanceData(TEST_TASK_ARN, TEST_ACCOUNT_ID, InstanceState.INITIALIZING));
    PublishedMessage taskLifecycleMessage = getTaskLifecycleMessage(
        INSTANCE_START_TIMESTAMP, EVENT_TYPE_START, TEST_TASK_ARN, TEST_ACCOUNT_ID, TEST_CLUSTER_ID);
    ecsTaskLifecycleWriter.write(Arrays.asList(taskLifecycleMessage));
    ArgumentCaptor<InstanceData> instanceDataArgumentCaptor = ArgumentCaptor.forClass(InstanceData.class);
    ArgumentCaptor<InstanceState> ec2InstanceStateArgumentCaptor = ArgumentCaptor.forClass(InstanceState.class);
    ArgumentCaptor<Instant> ec2InstanceStartTimeArgumentCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(instanceDataService)
        .updateInstanceState(instanceDataArgumentCaptor.capture(), ec2InstanceStartTimeArgumentCaptor.capture(),
            ec2InstanceStateArgumentCaptor.capture());
    assertThat(ec2InstanceStateArgumentCaptor.getValue()).isEqualTo(InstanceState.RUNNING);
    assertThat(ec2InstanceStartTimeArgumentCaptor.getValue())
        .isEqualTo(HTimestamps.toInstant(INSTANCE_START_TIMESTAMP));
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetFargateInstanceType() {
    PublishedMessage ec2InstanceInfoMessage = getTaskInfoMessage(TEST_TASK_ARN, TEST_SERVICE_NAME, FARGATE_LAUNCH_TYPE,
        TEST_CONTAINER_ARN, TEST_CLUSTER_ARN, TEST_ACCOUNT_ID, TEST_CLUSTER_ID);
    EcsTaskInfo ecsTaskInfo = (EcsTaskInfo) ec2InstanceInfoMessage.getMessage();
    InstanceType instanceType = ecsTaskInfoWriter.getInstanceType(ecsTaskInfo.getEcsTaskDescription());
    assertThat(instanceType).isEqualTo(InstanceType.ECS_TASK_FARGATE);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetNullInstanceType() {
    PublishedMessage ec2InstanceInfoMessage = getTaskInfoMessage(TEST_TASK_ARN, TEST_SERVICE_NAME, UNKNOWN_LAUNCH_TYPE,
        TEST_CONTAINER_ARN, TEST_CLUSTER_ARN, TEST_ACCOUNT_ID, TEST_CLUSTER_ID);
    EcsTaskInfo ecsTaskInfo = (EcsTaskInfo) ec2InstanceInfoMessage.getMessage();
    InstanceType nullInstanceType = ecsTaskInfoWriter.getInstanceType(ecsTaskInfo.getEcsTaskDescription());
    assertThat(nullInstanceType).isNull();
  }

  private Optional<HarnessServiceInfo> createHarnessServiceInfo() {
    return Optional.of(new HarnessServiceInfo(
        "serviceId", "appId", "cloudProviderId", "envId", "infraMappingId", "deploymentSummaryId"));
  }
}
