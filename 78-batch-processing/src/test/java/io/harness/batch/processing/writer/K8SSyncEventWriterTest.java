package io.harness.batch.processing.writer;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceEvent.EventType;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.category.element.UnitTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.watch.K8SClusterSyncEvent;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class K8SSyncEventWriterTest extends CategoryTest {
  private final String TEST_ACCOUNT_ID = "K8S_INSTANCE_INFO_ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ID = "K8S_TEST_CLUSTER_ID_" + this.getClass().getSimpleName();
  private final String CLOUD_PROVIDER_ID = "K8S_CLOUD_PROVIDER_ID_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID_POD_STOPPED =
      "K8S_INSTANCE_INFO_INSTANCE_ID_POD_STOPPED_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID_POD_RUNNING =
      "K8S_INSTANCE_INFO_INSTANCE_ID_POD_RUNNING_" + this.getClass().getSimpleName();
  private final String TEST_INSTANCE_ID_NODE_RUNNING =
      "K8S_INSTANCE_INFO_INSTANCE_ID_NODE_RUNNING_" + this.getClass().getSimpleName();
  private final String TEST_ACTIVE_INSTANCE_ID =
      "K8S_INSTANCE_INFO_ACTIVE_INSTANCE_ID_" + this.getClass().getSimpleName();

  @Autowired private ItemWriter<InstanceInfo> instanceInfoWriter;

  @Autowired private ItemWriter<InstanceEvent> instanceEventWriter;

  @Autowired private ItemWriter<PublishedMessage> k8sSyncEventWriter;

  @Autowired private InstanceDataService instanceDataService;

  @Autowired private HPersistence hPersistence;

  private final Instant NOW = Instant.now();
  private final Instant INSTANCE_STOP_TIMESTAMP = NOW.minus(1, ChronoUnit.DAYS);
  private final Instant INSTANCE_START_TIMESTAMP = NOW.minus(2, ChronoUnit.DAYS);

  @Before
  public void setUpData() throws Exception {
    InstanceInfo podStoppedInstanceInfo = fetchInstanceInfo(TEST_INSTANCE_ID_POD_STOPPED, InstanceType.K8S_POD);
    InstanceInfo podRunningInstanceInfo = fetchInstanceInfo(TEST_INSTANCE_ID_POD_RUNNING, InstanceType.K8S_POD);
    InstanceInfo nodeRunningInstanceInfo = fetchInstanceInfo(TEST_INSTANCE_ID_NODE_RUNNING, InstanceType.K8S_NODE);
    InstanceInfo nodeActiveInstanceInfo = fetchInstanceInfo(TEST_ACTIVE_INSTANCE_ID, InstanceType.K8S_NODE);
    instanceInfoWriter.write(
        Arrays.asList(podStoppedInstanceInfo, podRunningInstanceInfo, nodeRunningInstanceInfo, nodeActiveInstanceInfo));

    InstanceEvent podRunningInstanceEvent =
        fetchInstanceEvent(TEST_INSTANCE_ID_POD_STOPPED, INSTANCE_START_TIMESTAMP, EventType.START);
    InstanceEvent podStoppedInstanceEvent =
        fetchInstanceEvent(TEST_INSTANCE_ID_POD_STOPPED, INSTANCE_STOP_TIMESTAMP, EventType.STOP);
    InstanceEvent podTwoRunningInstanceEvent =
        fetchInstanceEvent(TEST_INSTANCE_ID_POD_RUNNING, INSTANCE_START_TIMESTAMP, EventType.START);
    InstanceEvent nodeRunningInstanceEvent =
        fetchInstanceEvent(TEST_INSTANCE_ID_NODE_RUNNING, INSTANCE_START_TIMESTAMP, EventType.START);
    InstanceEvent nodeActiveInstanceEvent =
        fetchInstanceEvent(TEST_ACTIVE_INSTANCE_ID, INSTANCE_START_TIMESTAMP, EventType.START);
    instanceEventWriter.write(Arrays.asList(podRunningInstanceEvent, podStoppedInstanceEvent,
        podTwoRunningInstanceEvent, nodeRunningInstanceEvent, nodeActiveInstanceEvent));
  }

  private InstanceInfo fetchInstanceInfo(String instanceId, InstanceType instanceType) {
    return InstanceInfo.builder()
        .accountId(TEST_ACCOUNT_ID)
        .settingId(CLOUD_PROVIDER_ID)
        .instanceType(instanceType)
        .instanceId(instanceId)
        .instanceName("instanceName")
        .clusterId(TEST_CLUSTER_ID)
        .clusterName("clusterName")
        .instanceState(InstanceState.INITIALIZING)
        .build();
  }

  private InstanceEvent fetchInstanceEvent(String instanceId, Instant instant, EventType eventType) {
    return InstanceEvent.builder()
        .accountId(TEST_ACCOUNT_ID)
        .cloudProviderId(CLOUD_PROVIDER_ID)
        .instanceId(instanceId)
        .type(eventType)
        .timestamp(instant)
        .build();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldStopK8SInstances() throws Exception {
    k8sSyncEventWriter.write(Arrays.asList(k8sSyncEvent()));

    List<InstanceState> stoppedInstanceState = new ArrayList<>(Arrays.asList(InstanceState.STOPPED));
    InstanceData stoppedInstanceData = instanceDataService.fetchActiveInstanceData(
        TEST_ACCOUNT_ID, TEST_INSTANCE_ID_POD_RUNNING, stoppedInstanceState);
    assertThat(stoppedInstanceData).isNotNull();
    assertThat(stoppedInstanceData.getUsageStartTime()).isEqualTo(INSTANCE_START_TIMESTAMP);
    assertThat(stoppedInstanceData.getUsageStopTime()).isEqualTo(NOW);

    List<InstanceState> activeInstanceState =
        new ArrayList<>(Arrays.asList(InstanceState.INITIALIZING, InstanceState.RUNNING));
    InstanceData activeInstanceData =
        instanceDataService.fetchActiveInstanceData(TEST_ACCOUNT_ID, TEST_ACTIVE_INSTANCE_ID, activeInstanceState);
    assertThat(activeInstanceData).isNotNull();
  }

  private PublishedMessage k8sSyncEvent() {
    K8SClusterSyncEvent k8SClusterSyncEvent = K8SClusterSyncEvent.newBuilder()
                                                  .setClusterId(TEST_CLUSTER_ID)
                                                  .setCloudProviderId(CLOUD_PROVIDER_ID)
                                                  .addAllActiveNodeUids(Arrays.asList(TEST_ACTIVE_INSTANCE_ID))
                                                  .setLastProcessedTimestamp(HTimestamps.fromInstant(NOW))
                                                  .build();
    Any payload = Any.pack(k8SClusterSyncEvent);
    return PublishedMessage.builder()
        .accountId(TEST_ACCOUNT_ID)
        .data(payload.toByteArray())
        .type(k8SClusterSyncEvent.getClass().getName())
        .build();
  }

  @After
  public void clearCollection() {
    val instanceDataStore = hPersistence.getDatastore(InstanceData.class);
    instanceDataStore.delete(
        instanceDataStore.createQuery(InstanceData.class).filter(InstanceDataKeys.accountId, TEST_ACCOUNT_ID));
  }
}
