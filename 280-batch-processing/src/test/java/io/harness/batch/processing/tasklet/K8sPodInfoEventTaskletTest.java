package io.harness.batch.processing.tasklet;

import static io.harness.ccm.cluster.entities.K8sWorkload.encodeDotsInKey;
import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.data.PrunedInstanceData;
import io.harness.batch.processing.billing.writer.support.ClusterDataGenerationValidator;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.dao.intfc.PublishedMessageDao;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.service.intfc.WorkloadRepository;
import io.harness.batch.processing.tasklet.support.HarnessServiceInfoFetcher;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.batch.processing.writer.constants.K8sCCMConstants;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.HarnessServiceInfo;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.ccm.commons.entities.InstanceData;
import io.harness.event.grpc.PublishedMessage;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.watch.PodEvent;
import io.harness.perpetualtask.k8s.watch.PodEvent.EventType;
import io.harness.perpetualtask.k8s.watch.PodInfo;
import io.harness.perpetualtask.k8s.watch.Quantity;
import io.harness.perpetualtask.k8s.watch.Resource;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.security.authentication.BatchQueryConfig;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;

@RunWith(MockitoJUnitRunner.class)
public class K8sPodInfoEventTaskletTest extends CategoryTest {
  @InjectMocks private K8sPodEventTasklet k8sPodEventTasklet;
  @InjectMocks private K8sPodInfoTasklet k8sPodInfoTasklet;
  @Mock private BatchMainConfig config;
  @Mock private HPersistence hPersistence;
  @Mock private InstanceDataDao instanceDataDao;
  @Mock private WorkloadRepository workloadRepository;
  @Mock private InstanceDataService instanceDataService;
  @Mock private PublishedMessageDao publishedMessageDao;
  @Mock private HarnessServiceInfoFetcher harnessServiceInfoFetcher;
  @Mock private ClusterDataGenerationValidator clusterDataGenerationValidator;

  private static final String POD_UID = "pod_uid";
  private static final String POD_NAME = "pod_name";
  private static final String NODE_POOL_NAME = "manager-pool";
  private static final String NAMESPACE = "namespace";
  private static final String KUBE_SYSTEM_NAMESPACE = "kube-system";
  private static final String KUBE_PROXY_POD_NAME = "kube-proxy-pod";
  private static final long CPU_AMOUNT = 1_000_000_000L; // 1 vcpu in nanocores
  private static final long MEMORY_AMOUNT = 1024L * 1024; // 1Mi in bytes
  private static final long CPU_LIMIT_AMOUNT = 2_000_000_000L;
  private static final long MEMORY_LIMIT_AMOUNT = 1024L * 1024 * 2;
  private static final String NODE_NAME = "node_name";
  private static final String CLOUD_PROVIDER_ID = "cloud_provider_id";
  private static final String CLOUD_PROVIDER_INSTANCE_ID = "cloud_provider_instance_id";
  private static final String ACCOUNT_ID = "account_id";
  private static final String CLUSTER_ID = "cluster_id";
  private static final String CLUSTER_NAME = "cluster_name";
  private static final String WORKLOAD_NAME = "workload_name";
  private static final String WORKLOAD_TYPE = "workload_type";
  private static final String WORKLOAD_ID = "workload_id";
  private static final String MAP_KEY_WITH_DOT = "harness.io/created.by";
  private static final String MAP_VALUE = "harness.io/created.by";
  Map<String, String> NAMESPACE_LABELS = ImmutableMap.of(MAP_KEY_WITH_DOT, MAP_VALUE);

  private final Instant NOW = Instant.now();
  private final Timestamp START_TIMESTAMP = HTimestamps.fromInstant(NOW.minus(1, ChronoUnit.DAYS));
  private final long START_TIME_MILLIS = NOW.minus(1, ChronoUnit.HOURS).toEpochMilli();
  private final long END_TIME_MILLIS = NOW.toEpochMilli();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(config.getBatchQueryConfig()).thenReturn(BatchQueryConfig.builder().queryBatchSize(50).build());
    when(clusterDataGenerationValidator.shouldGenerateClusterData(any(), any())).thenReturn(true);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testPodInfoExecute() {
    ChunkContext chunkContext = mock(ChunkContext.class);
    StepContext stepContext = mock(StepContext.class);
    StepExecution stepExecution = mock(StepExecution.class);
    JobParameters parameters = mock(JobParameters.class);

    when(chunkContext.getStepContext()).thenReturn(stepContext);
    when(stepContext.getStepExecution()).thenReturn(stepExecution);
    when(stepExecution.getJobParameters()).thenReturn(parameters);

    when(parameters.getString(CCMJobConstants.JOB_START_DATE)).thenReturn(String.valueOf(START_TIME_MILLIS));
    when(parameters.getString(CCMJobConstants.ACCOUNT_ID)).thenReturn(ACCOUNT_ID);
    when(parameters.getString(CCMJobConstants.JOB_END_DATE)).thenReturn(String.valueOf(END_TIME_MILLIS));

    PrunedInstanceData instanceData = getNodeInstantData();
    when(instanceDataService.fetchPrunedInstanceDataWithName(
             ACCOUNT_ID, CLUSTER_ID, NODE_NAME, HTimestamps.toMillis(START_TIMESTAMP)))
        .thenReturn(instanceData);
    Map<String, String> label = new HashMap<>();
    label.put(K8sCCMConstants.RELEASE_NAME, K8sCCMConstants.RELEASE_NAME);

    when(harnessServiceInfoFetcher.fetchHarnessServiceInfo(ACCOUNT_ID, CLOUD_PROVIDER_ID, NAMESPACE, POD_NAME, label))
        .thenReturn(harnessServiceInfo());
    Map<String, Quantity> requestQuantity = new HashMap<>();
    requestQuantity.put("cpu", getQuantity(CPU_AMOUNT, "M"));
    requestQuantity.put("memory", getQuantity(MEMORY_AMOUNT, "M"));
    Map<String, Quantity> limitQuantity = new HashMap<>();
    limitQuantity.put("cpu", getQuantity(CPU_LIMIT_AMOUNT, "M"));
    limitQuantity.put("memory", getQuantity(MEMORY_LIMIT_AMOUNT, "M"));
    Resource resource = Resource.newBuilder().putAllRequests(requestQuantity).putAllLimits(limitQuantity).build();
    PublishedMessage k8sPodInfoMessage =
        getK8sPodInfoMessage(POD_UID, POD_NAME, NODE_NAME, CLOUD_PROVIDER_ID, ACCOUNT_ID, CLUSTER_ID, CLUSTER_NAME,
            NAMESPACE, label, NAMESPACE_LABELS, resource, START_TIMESTAMP, WORKLOAD_NAME, WORKLOAD_TYPE, WORKLOAD_ID);

    when(publishedMessageDao.fetchPublishedMessage(any(), any(), any(), any(), anyInt()))
        .thenReturn(Arrays.asList(k8sPodInfoMessage));

    RepeatStatus repeatStatus = k8sPodInfoTasklet.execute(null, chunkContext);
    assertThat(repeatStatus).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testPodEventExecute() {
    ChunkContext chunkContext = mock(ChunkContext.class);
    StepContext stepContext = mock(StepContext.class);
    StepExecution stepExecution = mock(StepExecution.class);
    JobParameters parameters = mock(JobParameters.class);

    when(chunkContext.getStepContext()).thenReturn(stepContext);
    when(stepContext.getStepExecution()).thenReturn(stepExecution);
    when(stepExecution.getJobParameters()).thenReturn(parameters);

    when(parameters.getString(CCMJobConstants.JOB_START_DATE)).thenReturn(String.valueOf(START_TIME_MILLIS));
    when(parameters.getString(CCMJobConstants.ACCOUNT_ID)).thenReturn(ACCOUNT_ID);
    when(parameters.getString(CCMJobConstants.JOB_END_DATE)).thenReturn(String.valueOf(END_TIME_MILLIS));
    PublishedMessage k8sNodeEventMessage = getK8sPodEventMessage(
        POD_UID, CLOUD_PROVIDER_ID, CLUSTER_ID, ACCOUNT_ID, PodEvent.EventType.EVENT_TYPE_SCHEDULED, START_TIMESTAMP);
    when(publishedMessageDao.fetchPublishedMessage(any(), any(), any(), any(), anyInt()))
        .thenReturn(Arrays.asList(k8sNodeEventMessage));
    RepeatStatus repeatStatus = k8sPodEventTasklet.execute(null, chunkContext);
    assertThat(repeatStatus).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInstanceStartPodEvent() throws Exception {
    PublishedMessage k8sNodeEventMessage = getK8sPodEventMessage(
        POD_UID, CLOUD_PROVIDER_ID, CLUSTER_ID, ACCOUNT_ID, PodEvent.EventType.EVENT_TYPE_SCHEDULED, START_TIMESTAMP);
    InstanceEvent instanceEvent = k8sPodEventTasklet.process(k8sNodeEventMessage);
    assertThat(instanceEvent).isNotNull();
    assertThat(instanceEvent.getTimestamp()).isEqualTo(HTimestamps.toInstant(START_TIMESTAMP));
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInvalidInstancePodEvent() throws Exception {
    PublishedMessage k8sNodeEventMessage = getK8sPodEventMessage(
        POD_UID, CLOUD_PROVIDER_ID, CLUSTER_ID, ACCOUNT_ID, EventType.EVENT_TYPE_UNSPECIFIED, START_TIMESTAMP);
    InstanceEvent instanceEvent = k8sPodEventTasklet.process(k8sNodeEventMessage);
    assertThat(instanceEvent).isNotNull();
    assertThat(instanceEvent.getType()).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInstanceStopPodEvent() throws Exception {
    PublishedMessage k8sNodeEventMessage = getK8sPodEventMessage(
        POD_UID, CLOUD_PROVIDER_ID, CLUSTER_ID, ACCOUNT_ID, EventType.EVENT_TYPE_TERMINATED, START_TIMESTAMP);
    InstanceEvent instanceEvent = k8sPodEventTasklet.process(k8sNodeEventMessage);
    assertThat(instanceEvent).isNotNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateEmptyInstancePodInfo() throws Exception {
    InstanceData instanceData = InstanceData.builder().build();
    when(instanceDataService.fetchInstanceData(ACCOUNT_ID, CLUSTER_ID, POD_UID)).thenReturn(instanceData);
    PublishedMessage k8sPodInfoMessage = getK8sPodInfoMessage(POD_UID, POD_NAME, NODE_NAME, CLOUD_PROVIDER_ID,
        ACCOUNT_ID, CLUSTER_ID, CLUSTER_NAME, NAMESPACE, Collections.emptyMap(), Collections.emptyMap(),
        Resource.newBuilder().build(), START_TIMESTAMP, WORKLOAD_NAME, WORKLOAD_TYPE, WORKLOAD_ID);
    InstanceInfo instanceInfo = k8sPodInfoTasklet.process(k8sPodInfoMessage);
    assertThat(instanceInfo.getInstanceId()).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInstancePodInfo() throws Exception {
    PrunedInstanceData instanceData = getNodeInstantData();
    when(instanceDataService.fetchPrunedInstanceDataWithName(
             ACCOUNT_ID, CLUSTER_ID, NODE_NAME, HTimestamps.toMillis(START_TIMESTAMP)))
        .thenReturn(instanceData);
    Map<String, String> label = new HashMap<>();
    label.put(K8sCCMConstants.RELEASE_NAME, K8sCCMConstants.RELEASE_NAME);
    when(harnessServiceInfoFetcher.fetchHarnessServiceInfo(ACCOUNT_ID, CLOUD_PROVIDER_ID, NAMESPACE, POD_NAME, label))
        .thenReturn(harnessServiceInfo());
    Map<String, Quantity> requestQuantity = new HashMap<>();
    requestQuantity.put("cpu", getQuantity(CPU_AMOUNT, "M"));
    requestQuantity.put("memory", getQuantity(MEMORY_AMOUNT, "M"));
    Map<String, Quantity> limitQuantity = new HashMap<>();
    limitQuantity.put("cpu", getQuantity(CPU_LIMIT_AMOUNT, "M"));
    limitQuantity.put("memory", getQuantity(MEMORY_LIMIT_AMOUNT, "M"));
    Resource resource = Resource.newBuilder().putAllRequests(requestQuantity).putAllLimits(limitQuantity).build();
    PublishedMessage k8sPodInfoMessage =
        getK8sPodInfoMessage(POD_UID, POD_NAME, NODE_NAME, CLOUD_PROVIDER_ID, ACCOUNT_ID, CLUSTER_ID, CLUSTER_NAME,
            NAMESPACE, label, NAMESPACE_LABELS, resource, START_TIMESTAMP, WORKLOAD_NAME, WORKLOAD_TYPE, WORKLOAD_ID);
    InstanceInfo instanceInfo = k8sPodInfoTasklet.process(k8sPodInfoMessage);
    io.harness.ccm.commons.beans.Resource infoResource = instanceInfo.getResource();
    io.harness.ccm.commons.beans.Resource limitResource = instanceInfo.getResourceLimit();
    Map<String, String> metaData = instanceInfo.getMetaData();
    assertThat(instanceInfo).isNotNull();
    assertThat(instanceInfo.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(instanceInfo.getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(instanceInfo.getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(instanceInfo.getInstanceType()).isEqualTo(InstanceType.K8S_POD);
    assertThat(infoResource.getMemoryMb()).isEqualTo(1.0);
    assertThat(infoResource.getCpuUnits()).isEqualTo(1024.0);
    assertThat(limitResource.getMemoryMb()).isEqualTo(2.0);
    assertThat(limitResource.getCpuUnits()).isEqualTo(2048.0);
    assertThat(metaData.get(InstanceMetaDataConstants.WORKLOAD_NAME)).isEqualTo(WORKLOAD_NAME);
    assertThat(metaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_MEMORY))
        .isEqualTo(String.valueOf((double) MEMORY_AMOUNT));
    assertThat(metaData.get(InstanceMetaDataConstants.CLOUD_PROVIDER_INSTANCE_ID))
        .isEqualTo(CLOUD_PROVIDER_INSTANCE_ID);
    assertThat(metaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_CPU))
        .isEqualTo(String.valueOf((double) CPU_AMOUNT));
    assertThat(metaData.get(InstanceMetaDataConstants.NODE_POOL_NAME)).isEqualTo(NODE_POOL_NAME);
    assertThat(instanceInfo.getNamespaceLabels()).isEqualTo(encodeDotsInKey(NAMESPACE_LABELS));
    assertThat(instanceInfo.getNamespaceLabels()).isNotEqualTo(NAMESPACE_LABELS);
    verify(workloadRepository).savePodWorkload(ACCOUNT_ID, (PodInfo) k8sPodInfoMessage.getMessage());
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldCreateInstancePodInfoWithKubeProxyWorkload() throws Exception {
    PrunedInstanceData instanceData = getNodeInstantData();
    when(instanceDataService.fetchPrunedInstanceDataWithName(
             ACCOUNT_ID, CLUSTER_ID, NODE_NAME, HTimestamps.toMillis(START_TIMESTAMP)))
        .thenReturn(instanceData);
    Map<String, String> label = new HashMap<>();
    label.put(K8sCCMConstants.RELEASE_NAME, K8sCCMConstants.RELEASE_NAME);
    when(harnessServiceInfoFetcher.fetchHarnessServiceInfo(
             ACCOUNT_ID, CLOUD_PROVIDER_ID, KUBE_SYSTEM_NAMESPACE, KUBE_PROXY_POD_NAME, label))
        .thenReturn(harnessServiceInfo());
    Map<String, Quantity> requestQuantity = new HashMap<>();
    requestQuantity.put("cpu", getQuantity(CPU_AMOUNT, "M"));
    requestQuantity.put("memory", getQuantity(MEMORY_AMOUNT, "M"));
    Resource resource = Resource.newBuilder().putAllRequests(requestQuantity).build();
    PublishedMessage k8sPodInfoMessage =
        getK8sPodInfoMessage(POD_UID, KUBE_PROXY_POD_NAME, NODE_NAME, CLOUD_PROVIDER_ID, ACCOUNT_ID, CLUSTER_ID,
            CLUSTER_NAME, KUBE_SYSTEM_NAMESPACE, label, NAMESPACE_LABELS, resource, START_TIMESTAMP, "", "", "");
    InstanceInfo instanceInfo = k8sPodInfoTasklet.process(k8sPodInfoMessage);
    io.harness.ccm.commons.beans.Resource infoResource = instanceInfo.getResource();
    io.harness.ccm.commons.beans.Resource limitResource = instanceInfo.getResourceLimit();

    Map<String, String> metaData = instanceInfo.getMetaData();
    assertThat(instanceInfo).isNotNull();
    assertThat(instanceInfo.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(instanceInfo.getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(instanceInfo.getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(instanceInfo.getInstanceType()).isEqualTo(InstanceType.K8S_POD);
    assertThat(infoResource.getCpuUnits()).isEqualTo(1024.0);
    assertThat(infoResource.getMemoryMb()).isEqualTo(1.0);
    assertThat(limitResource.getCpuUnits()).isEqualTo(0.0);
    assertThat(limitResource.getMemoryMb()).isEqualTo(0.0);
    assertThat(metaData.get(InstanceMetaDataConstants.WORKLOAD_NAME)).isEqualTo("kube-proxy");
    assertThat(metaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_MEMORY))
        .isEqualTo(String.valueOf((double) MEMORY_AMOUNT));
    assertThat(metaData.get(InstanceMetaDataConstants.PARENT_RESOURCE_CPU))
        .isEqualTo(String.valueOf((double) CPU_AMOUNT));
    verify(workloadRepository).savePodWorkload(ACCOUNT_ID, (PodInfo) k8sPodInfoMessage.getMessage());
  }

  private PrunedInstanceData getNodeInstantData() {
    Map<String, String> nodeMetaData = new HashMap<>();
    nodeMetaData.put(InstanceMetaDataConstants.REGION, InstanceMetaDataConstants.REGION);
    nodeMetaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY, InstanceMetaDataConstants.INSTANCE_FAMILY);
    nodeMetaData.put(InstanceMetaDataConstants.OPERATING_SYSTEM, InstanceMetaDataConstants.OPERATING_SYSTEM);
    nodeMetaData.put(K8sCCMConstants.GKE_NODE_POOL_KEY, NODE_POOL_NAME);
    io.harness.ccm.commons.beans.Resource instanceResource = io.harness.ccm.commons.beans.Resource.builder()
                                                                 .cpuUnits((double) CPU_AMOUNT)
                                                                 .memoryMb((double) MEMORY_AMOUNT)
                                                                 .build();
    return PrunedInstanceData.builder()
        .instanceId(NODE_NAME)
        .cloudProviderInstanceId(CLOUD_PROVIDER_INSTANCE_ID)
        .totalResource(instanceResource)
        .metaData(nodeMetaData)
        .build();
  }

  private Quantity getQuantity(long amount, String unit) {
    return Quantity.newBuilder().setAmount(amount).setUnit(unit).build();
  }

  private PublishedMessage getK8sPodInfoMessage(String podUid, String podName, String nodeName, String cloudProviderId,
      String accountId, String clusterId, String clusterName, String namespace, Map<String, String> label,
      Map<String, String> namespaceLabels, Resource resource, Timestamp timestamp, String workloadName,
      String workloadType, String workloadId) {
    PodInfo nodeInfo = PodInfo.newBuilder()
                           .setPodUid(podUid)
                           .setPodName(podName)
                           .setNodeName(nodeName)
                           .setCloudProviderId(cloudProviderId)
                           .setClusterId(clusterId)
                           .setClusterName(clusterName)
                           .setNamespace(namespace)
                           .putAllLabels(label)
                           .putAllNamespaceLabels(namespaceLabels)
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
