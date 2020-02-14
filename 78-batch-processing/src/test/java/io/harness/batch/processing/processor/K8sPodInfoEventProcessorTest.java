package io.harness.batch.processing.processor;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.service.intfc.WorkloadRepository;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.batch.processing.writer.constants.K8sCCMConstants;
import io.harness.category.element.UnitTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.watch.PodEvent;
import io.harness.perpetualtask.k8s.watch.PodEvent.EventType;
import io.harness.perpetualtask.k8s.watch.PodInfo;
import io.harness.perpetualtask.k8s.watch.Resource;
import io.harness.perpetualtask.k8s.watch.Resource.Quantity;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class K8sPodInfoEventProcessorTest extends CategoryTest {
  @InjectMocks private K8sPodEventProcessor k8sPodEventProcessor;
  @InjectMocks private K8sPodInfoProcessor k8sPodInfoProcessor;
  @Mock private InstanceDataService instanceDataService;
  @Mock private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Mock private HPersistence hPersistence;
  @Mock private WorkloadRepository workloadRepository;

  private static final String POD_UID = "pod_uid";
  private static final String POD_NAME = "pod_name";
  private static final String NAMESPACE = "namespace";
  private static final String KUBE_SYSTEM_NAMESPACE = "kube-system";
  private static final String KUBE_PROXY_POD_NAME = "kube-proxy-pod";
  private static final long CPU_AMOUNT = 1_000_000_000L; // 1 vcpu in nanocores
  private static final long MEMORY_AMOUNT = 1024L * 1024; // 1Mi in bytes
  private static final String NODE_NAME = "node_name";
  private static final String CLOUD_PROVIDER_ID = "cloud_provider_id";
  private static final String ACCOUNT_ID = "account_id";
  private static final String CLUSTER_ID = "cluster_id";
  private static final String CLUSTER_NAME = "cluster_name";
  private static final String WORKLOAD_NAME = "workload_name";
  private static final String WORKLOAD_TYPE = "workload_type";
  private static final String WORKLOAD_ID = "workload_id";
  private final Instant NOW = Instant.now();
  private final Timestamp START_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(1, ChronoUnit.DAYS));

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInstanceStartPodEvent() throws Exception {
    PublishedMessage k8sNodeEventMessage = getK8sPodEventMessage(
        POD_UID, CLOUD_PROVIDER_ID, CLUSTER_ID, ACCOUNT_ID, PodEvent.EventType.EVENT_TYPE_SCHEDULED, START_TIMESTAMP);
    InstanceEvent instanceEvent = k8sPodEventProcessor.process(k8sNodeEventMessage);
    assertThat(instanceEvent).isNotNull();
    assertThat(instanceEvent.getTimestamp()).isEqualTo(HTimestamps.toInstant(START_TIMESTAMP));
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInvalidInstancePodEvent() throws Exception {
    PublishedMessage k8sNodeEventMessage = getK8sPodEventMessage(
        POD_UID, CLOUD_PROVIDER_ID, CLUSTER_ID, ACCOUNT_ID, EventType.EVENT_TYPE_INVALID, START_TIMESTAMP);
    InstanceEvent instanceEvent = k8sPodEventProcessor.process(k8sNodeEventMessage);
    assertThat(instanceEvent).isNotNull();
    assertThat(instanceEvent.getType()).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInstanceStopPodEvent() throws Exception {
    PublishedMessage k8sNodeEventMessage = getK8sPodEventMessage(
        POD_UID, CLOUD_PROVIDER_ID, CLUSTER_ID, ACCOUNT_ID, EventType.EVENT_TYPE_DELETED, START_TIMESTAMP);
    InstanceEvent instanceEvent = k8sPodEventProcessor.process(k8sNodeEventMessage);
    assertThat(instanceEvent).isNotNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInstancePodInfo() throws Exception {
    InstanceData instanceData = getNodeInstantData();
    when(instanceDataService.fetchInstanceDataWithName(
             ACCOUNT_ID, CLUSTER_ID, NODE_NAME, HTimestamps.toMillis(START_TIMESTAMP)))
        .thenReturn(instanceData);
    when(cloudToHarnessMappingService.getHarnessServiceInfo(any())).thenReturn(harnessServiceInfo());
    Map<String, String> label = new HashMap<>();
    label.put(K8sCCMConstants.RELEASE_NAME, K8sCCMConstants.RELEASE_NAME);
    Map<String, Quantity> requestQuantity = new HashMap<>();
    requestQuantity.put("cpu", getQuantity(CPU_AMOUNT, "M"));
    requestQuantity.put("memory", getQuantity(MEMORY_AMOUNT, "M"));
    Resource resource = Resource.newBuilder().putAllRequests(requestQuantity).build();
    PublishedMessage k8sPodInfoMessage =
        getK8sPodInfoMessage(POD_UID, POD_NAME, NODE_NAME, CLOUD_PROVIDER_ID, ACCOUNT_ID, CLUSTER_ID, CLUSTER_NAME,
            NAMESPACE, label, resource, START_TIMESTAMP, WORKLOAD_NAME, WORKLOAD_TYPE, WORKLOAD_ID);
    InstanceInfo instanceInfo = k8sPodInfoProcessor.process(k8sPodInfoMessage);
    io.harness.batch.processing.ccm.Resource infoResource = instanceInfo.getResource();
    Map<String, String> metaData = instanceInfo.getMetaData();
    assertThat(instanceInfo).isNotNull();
    assertThat(instanceInfo.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(instanceInfo.getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(instanceInfo.getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(instanceInfo.getInstanceType()).isEqualTo(InstanceType.K8S_POD);
    assertThat(infoResource.getCpuUnits()).isEqualTo(1024.0);
    assertThat(infoResource.getMemoryMb()).isEqualTo(1.0);
    assertThat(metaData.get(InstanceMetaDataConstants.WORKLOAD_NAME)).isEqualTo(WORKLOAD_NAME);
    assertThat(metaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_MEMORY))
        .isEqualTo(String.valueOf((double) MEMORY_AMOUNT));
    assertThat(metaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_CPU))
        .isEqualTo(String.valueOf((double) CPU_AMOUNT));
    verify(workloadRepository).savePodWorkload(ACCOUNT_ID, (PodInfo) k8sPodInfoMessage.getMessage());
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInstancePodInfoWithKubeProxyWorkload() throws Exception {
    InstanceData instanceData = getNodeInstantData();
    when(instanceDataService.fetchInstanceDataWithName(
             ACCOUNT_ID, CLUSTER_ID, NODE_NAME, HTimestamps.toMillis(START_TIMESTAMP)))
        .thenReturn(instanceData);
    when(cloudToHarnessMappingService.getHarnessServiceInfo(any())).thenReturn(harnessServiceInfo());
    Map<String, String> label = new HashMap<>();
    label.put(K8sCCMConstants.RELEASE_NAME, K8sCCMConstants.RELEASE_NAME);
    Map<String, Quantity> requestQuantity = new HashMap<>();
    requestQuantity.put("cpu", getQuantity(CPU_AMOUNT, "M"));
    requestQuantity.put("memory", getQuantity(MEMORY_AMOUNT, "M"));
    Resource resource = Resource.newBuilder().putAllRequests(requestQuantity).build();
    PublishedMessage k8sPodInfoMessage =
        getK8sPodInfoMessage(POD_UID, KUBE_PROXY_POD_NAME, NODE_NAME, CLOUD_PROVIDER_ID, ACCOUNT_ID, CLUSTER_ID,
            CLUSTER_NAME, KUBE_SYSTEM_NAMESPACE, label, resource, START_TIMESTAMP, "", "", "");
    InstanceInfo instanceInfo = k8sPodInfoProcessor.process(k8sPodInfoMessage);
    io.harness.batch.processing.ccm.Resource infoResource = instanceInfo.getResource();
    Map<String, String> metaData = instanceInfo.getMetaData();
    assertThat(instanceInfo).isNotNull();
    assertThat(instanceInfo.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(instanceInfo.getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(instanceInfo.getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(instanceInfo.getInstanceType()).isEqualTo(InstanceType.K8S_POD);
    assertThat(infoResource.getCpuUnits()).isEqualTo(1024.0);
    assertThat(infoResource.getMemoryMb()).isEqualTo(1.0);
    assertThat(metaData.get(InstanceMetaDataConstants.WORKLOAD_NAME)).isEqualTo("kube-proxy");
    assertThat(metaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_MEMORY))
        .isEqualTo(String.valueOf((double) MEMORY_AMOUNT));
    assertThat(metaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_CPU))
        .isEqualTo(String.valueOf((double) CPU_AMOUNT));
    verify(workloadRepository).savePodWorkload(ACCOUNT_ID, (PodInfo) k8sPodInfoMessage.getMessage());
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetHarnessServiceInfo() {
    HarnessServiceInfo harnessServiceInfo = k8sPodInfoProcessor.getHarnessServiceInfo(ACCOUNT_ID, new HashMap<>());
    assertThat(harnessServiceInfo).isNull();
  }

  private InstanceData getNodeInstantData() {
    Map<String, String> nodeMetaData = new HashMap<>();
    nodeMetaData.put(InstanceMetaDataConstants.REGION, InstanceMetaDataConstants.REGION);
    nodeMetaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY, InstanceMetaDataConstants.INSTANCE_FAMILY);
    nodeMetaData.put(InstanceMetaDataConstants.OPERATING_SYSTEM, InstanceMetaDataConstants.OPERATING_SYSTEM);
    io.harness.batch.processing.ccm.Resource instanceResource = io.harness.batch.processing.ccm.Resource.builder()
                                                                    .cpuUnits(Double.valueOf(CPU_AMOUNT))
                                                                    .memoryMb(Double.valueOf(MEMORY_AMOUNT))
                                                                    .build();
    return InstanceData.builder()
        .instanceId(NODE_NAME)
        .instanceType(InstanceType.K8S_NODE)
        .totalResource(instanceResource)
        .metaData(nodeMetaData)
        .build();
  }

  private Quantity getQuantity(long amount, String unit) {
    return Quantity.newBuilder().setAmount(amount).setUnit(unit).build();
  }

  private PublishedMessage getK8sPodInfoMessage(String podUid, String podName, String nodeName, String cloudProviderId,
      String accountId, String clusterId, String clusterName, String namespace, Map<String, String> label,
      Resource resource, Timestamp timestamp, String workloadName, String workloadType, String workloadId) {
    PodInfo nodeInfo = PodInfo.newBuilder()
                           .setPodUid(podUid)
                           .setPodName(podName)
                           .setNodeName(nodeName)
                           .setCloudProviderId(cloudProviderId)
                           .setClusterId(clusterId)
                           .setClusterName(clusterName)
                           .setNamespace(namespace)
                           .putAllLabels(label)
                           .setTotalResource(resource)
                           .setCreationTimestamp(timestamp)
                           .setTopLevelOwner(getOwner(workloadName, workloadType, workloadId))
                           .build();
    return getPublishedMessage(accountId, nodeInfo, HTimestamps.toMillis(timestamp));
  }

  private io.harness.perpetualtask.k8s.watch.Owner getOwner(
      String workloadName, String workloadType, String workloadId) {
    return io.harness.perpetualtask.k8s.watch.Owner.newBuilder()
        .setName(workloadName)
        .setKind(workloadType)
        .setUid(workloadId)
        .build();
  }

  private PublishedMessage getK8sPodEventMessage(String PodUid, String cloudProviderId, String clusterId,
      String accountId, PodEvent.EventType eventType, Timestamp timestamp) {
    PodEvent podEvent = PodEvent.newBuilder()
                            .setPodUid(PodUid)
                            .setCloudProviderId(cloudProviderId)
                            .setClusterId(clusterId)
                            .setType(eventType)
                            .setTimestamp(timestamp)
                            .build();
    return getPublishedMessage(accountId, podEvent, HTimestamps.toMillis(timestamp));
  }

  private PublishedMessage getPublishedMessage(String accountId, Message message, long occurredAt) {
    Any payload = Any.pack(message);
    return PublishedMessage.builder()
        .data(payload.toByteArray())
        .occurredAt(occurredAt)
        .type(message.getClass().getName())
        .accountId(accountId)
        .build();
  }

  private Optional<HarnessServiceInfo> harnessServiceInfo() {
    return Optional.of(new HarnessServiceInfo(
        "serviceId", "appId", "cloudProviderId", "envId", "infraMappingId", "deploymentSummaryId"));
  }
}
