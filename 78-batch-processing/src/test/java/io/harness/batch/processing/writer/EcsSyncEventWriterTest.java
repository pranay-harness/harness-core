package io.harness.batch.processing.writer;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Timestamp;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.integration.EcsEventGenerator;
import io.harness.batch.processing.service.intfc.InstanceDataService;
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
import software.wings.beans.instance.HarnessServiceInfo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class EcsSyncEventWriterTest extends CategoryTest implements EcsEventGenerator {
  @InjectMocks private EcsSyncEventWriter ecsSyncEventWriter;
  @Mock private InstanceDataService instanceDataService;

  private final String TEST_CLUSTER_ID = "CLUSTER_ID_" + this.getClass().getSimpleName();
  private final String TEST_ACCOUNT_ID = "ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_SETTING_ID = "SETTING_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ARN = "CLUSTER_ARN_" + this.getClass().getSimpleName();
  private final String TEST_ACTIVE_CONTAINER_ARN = "ACTIVE_CONTAINER_ARN_" + this.getClass().getSimpleName();
  private final String TEST_ACTIVE_INSTANCE_ID =
      "EC2_INSTANCE_INFO_ACTIVE_INSTANCE_ID_" + this.getClass().getSimpleName();

  private final Instant NOW = Instant.now();
  private final Timestamp INSTANCE_LAST_PROCESSED_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(1, ChronoUnit.DAYS));

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldStopInactiveInstance() throws Exception {
    when(instanceDataService.fetchClusterActiveInstanceData(
             TEST_ACCOUNT_ID, TEST_CLUSTER_ID, HTimestamps.toInstant(INSTANCE_LAST_PROCESSED_TIMESTAMP)))
        .thenReturn(Arrays.asList(
            createContainerInstanceData(TEST_ACTIVE_CONTAINER_ARN, TEST_ACCOUNT_ID, InstanceState.RUNNING)));
    when(instanceDataService.fetchActiveInstanceData(
             TEST_ACCOUNT_ID, TEST_CLUSTER_ID, TEST_ACTIVE_CONTAINER_ARN, Arrays.asList(InstanceState.RUNNING)))
        .thenReturn(createContainerInstanceData(TEST_ACTIVE_CONTAINER_ARN, TEST_ACCOUNT_ID, InstanceState.RUNNING));
    List<String> activeEc2InstanceList = new ArrayList<>(Arrays.asList(TEST_ACTIVE_INSTANCE_ID));
    PublishedMessage ecsSyncEventMessage =
        getEcsSyncEventMessage(TEST_ACCOUNT_ID, TEST_SETTING_ID, TEST_CLUSTER_ID, TEST_CLUSTER_ARN,
            Collections.emptyList(), activeEc2InstanceList, Collections.emptyList(), INSTANCE_LAST_PROCESSED_TIMESTAMP);
    ecsSyncEventWriter.write(Arrays.asList(ecsSyncEventMessage));
    ArgumentCaptor<InstanceData> instanceDataArgumentCaptor = ArgumentCaptor.forClass(InstanceData.class);
    ArgumentCaptor<InstanceState> instanceStateArgumentCaptor = ArgumentCaptor.forClass(InstanceState.class);
    ArgumentCaptor<Instant> endTimeArgumentCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(instanceDataService)
        .updateInstanceState(instanceDataArgumentCaptor.capture(), endTimeArgumentCaptor.capture(),
            instanceStateArgumentCaptor.capture());
    InstanceData instanceData = instanceDataArgumentCaptor.getValue();
    assertThat(instanceData.getInstanceId()).isEqualTo(TEST_ACTIVE_CONTAINER_ARN);
    assertThat(instanceStateArgumentCaptor.getValue()).isEqualTo(InstanceState.STOPPED);
    assertThat(endTimeArgumentCaptor.getValue()).isEqualTo(HTimestamps.toInstant(INSTANCE_LAST_PROCESSED_TIMESTAMP));
  }

  private Optional<HarnessServiceInfo> createHarnessServiceInfo() {
    return Optional.of(new HarnessServiceInfo("serviceId", "appId", "cloudProviderId", "envId", "infraMappingId"));
  }
}
