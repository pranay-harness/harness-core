package io.harness.batch.processing.config;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.ClusterType;
import io.harness.batch.processing.ccm.CostEventSource;
import io.harness.batch.processing.ccm.CostEventType;
import io.harness.batch.processing.ccm.EnrichedEvent;
import io.harness.batch.processing.events.timeseries.data.CostEventData;
import io.harness.batch.processing.events.timeseries.service.intfc.CostEventService;
import io.harness.batch.processing.k8s.WatchEventCostEstimator;
import io.harness.batch.processing.processor.support.K8sLabelServiceInfoFetcher;
import io.harness.batch.processing.service.intfc.WorkloadRepository;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.dao.K8sYamlDao;
import io.harness.ccm.cluster.entities.K8sWorkload;
import io.harness.ccm.cluster.entities.K8sYaml;
import io.harness.event.grpc.PublishedMessage;
import io.harness.perpetualtask.k8s.watch.K8sObjectReference;
import io.harness.perpetualtask.k8s.watch.K8sWatchEvent;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import software.wings.beans.instance.HarnessServiceInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class K8sWatchEventConfigTest extends CategoryTest {
  private static final String CLUSTER_ID = "cluster-id";
  private static final String CLOUD_PROVIDER_ID = "cloud-provider-id";
  private static final String UID = "b10eed3e-5e02-11ea-a4b7-4201ac100a0a";
  private static final String ACCOUNT_ID = "6a0173e1-ca0a-4af6-ab3a-75eff3acffa5";
  private static final String CLUSTER_NAME = "cluster-name";

  private K8sYamlDao k8sYamlDao;
  private K8sWatchEventConfig k8sWatchEventConfig;
  private WorkloadRepository workloadRepository;
  private K8sLabelServiceInfoFetcher k8sLabelServiceInfoFetcher;
  private CostEventService costEventService;
  private WatchEventCostEstimator watchEventCostEstimator;

  @Before
  public void setUp() throws Exception {
    k8sYamlDao = mock(K8sYamlDao.class);
    workloadRepository = mock(WorkloadRepository.class);
    k8sLabelServiceInfoFetcher = mock(K8sLabelServiceInfoFetcher.class);
    k8sWatchEventConfig = new K8sWatchEventConfig(null, null);
    k8sLabelServiceInfoFetcher = mock(K8sLabelServiceInfoFetcher.class);
    costEventService = mock(CostEventService.class);
    watchEventCostEstimator = mock(WatchEventCostEstimator.class);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testNormalizer_shouldPassthroughIfTypeUpdated() throws Exception {
    ItemProcessor<PublishedMessage, PublishedMessage> normalizer = k8sWatchEventConfig.normalizer(k8sYamlDao);
    PublishedMessage message = PublishedMessage.builder()
                                   .message(K8sWatchEvent.newBuilder()
                                                .setType(K8sWatchEvent.Type.TYPE_UPDATED)
                                                .setClusterId(CLUSTER_ID)
                                                .setResourceRef(K8sObjectReference.newBuilder().setUid(UID).build())
                                                .build())
                                   .build();
    PublishedMessage processed = normalizer.process(message);
    assertThat(processed).isSameAs(message);
    verifyZeroInteractions(k8sYamlDao);
  }
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testNormalizer_shouldPassthroughIfTypeDeleted() throws Exception {
    ItemProcessor<PublishedMessage, PublishedMessage> normalizer = k8sWatchEventConfig.normalizer(k8sYamlDao);
    PublishedMessage message = PublishedMessage.builder()
                                   .accountId(ACCOUNT_ID)
                                   .message(K8sWatchEvent.newBuilder()
                                                .setType(K8sWatchEvent.Type.TYPE_DELETED)
                                                .setClusterId(CLUSTER_ID)
                                                .setResourceRef(K8sObjectReference.newBuilder().setUid(UID).build())
                                                .build())
                                   .build();
    PublishedMessage processed = normalizer.process(message);
    assertThat(processed).isSameAs(message);
    verifyZeroInteractions(k8sYamlDao);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testNormalizer_shouldPassThroughIfTrueAdded() throws Exception {
    when(k8sYamlDao.fetchLatestYaml(ACCOUNT_ID, CLUSTER_ID, UID)).thenReturn(null);
    ItemProcessor<PublishedMessage, PublishedMessage> normalizer = k8sWatchEventConfig.normalizer(k8sYamlDao);
    PublishedMessage message = PublishedMessage.builder()
                                   .accountId(ACCOUNT_ID)
                                   .message(K8sWatchEvent.newBuilder()
                                                .setType(K8sWatchEvent.Type.TYPE_ADDED)
                                                .setClusterId(CLUSTER_ID)
                                                .setResourceRef(K8sObjectReference.newBuilder().setUid(UID).build())
                                                .build())
                                   .build();
    PublishedMessage processed = normalizer.process(message);
    assertThat(processed).isSameAs(message);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testNormalizer_shouldReturnNullIfAlreadyAdded() throws Exception {
    when(k8sYamlDao.fetchLatestYaml(ACCOUNT_ID, CLUSTER_ID, UID))
        .thenReturn(K8sYaml.builder().uid(UID).clusterId(CLUSTER_ID).yaml("yaml").build());
    ItemProcessor<PublishedMessage, PublishedMessage> normalizer = k8sWatchEventConfig.normalizer(k8sYamlDao);
    PublishedMessage message = PublishedMessage.builder()
                                   .accountId(ACCOUNT_ID)
                                   .message(K8sWatchEvent.newBuilder()
                                                .setType(K8sWatchEvent.Type.TYPE_ADDED)
                                                .setClusterId(CLUSTER_ID)
                                                .setResourceRef(K8sObjectReference.newBuilder().setUid(UID).build())
                                                .setNewResourceYaml("yaml")
                                                .build())
                                   .build();
    PublishedMessage processed = normalizer.process(message);
    assertThat(processed).isNull();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testNormalizer_shouldConvertToUpdatedIfAlreadyAddedDifferentYaml() throws Exception {
    when(k8sYamlDao.fetchLatestYaml(ACCOUNT_ID, CLUSTER_ID, UID))
        .thenReturn(K8sYaml.builder().uid(UID).clusterId(CLUSTER_ID).resourceVersion("12334").yaml("yaml1").build());
    ItemProcessor<PublishedMessage, PublishedMessage> normalizer = k8sWatchEventConfig.normalizer(k8sYamlDao);
    PublishedMessage message = PublishedMessage.builder()
                                   .accountId(ACCOUNT_ID)
                                   .message(K8sWatchEvent.newBuilder()
                                                .setType(K8sWatchEvent.Type.TYPE_ADDED)
                                                .setClusterId(CLUSTER_ID)
                                                .setResourceRef(K8sObjectReference.newBuilder().setUid(UID).build())
                                                .setNewResourceVersion("12345")
                                                .setNewResourceYaml("yaml2")
                                                .build())
                                   .build();
    PublishedMessage processed = normalizer.process(message);
    assertThat(processed).isEqualTo(
        PublishedMessage.builder()
            .accountId(ACCOUNT_ID)
            .message(K8sWatchEvent.newBuilder()
                         .setType(K8sWatchEvent.Type.TYPE_UPDATED)
                         .setClusterId(CLUSTER_ID)
                         .setResourceRef(K8sObjectReference.newBuilder().setUid(UID).build())
                         .setOldResourceVersion("12334")
                         .setOldResourceYaml("yaml1")
                         .setNewResourceVersion("12345")
                         .setNewResourceYaml("yaml2")
                         .setDescription("Missed update event")
                         .build())
            .build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testEnricher_shouldMapToHarnessServiceInfo() throws Exception {
    Map<String, String> labelMap = ImmutableMap.of("label1", "value1", "label2", "value2");
    when(workloadRepository.getWorkload(ACCOUNT_ID, CLUSTER_ID, UID))
        .thenReturn(Optional.of(K8sWorkload.builder().labels(labelMap).build()));
    HarnessServiceInfo harnessServiceInfo = new HarnessServiceInfo(
        "svc-id", "app-id", "cloud-provider-id", "env-id", "infra-mapping-id", "deployment-summary-id");
    when(k8sLabelServiceInfoFetcher.fetchHarnessServiceInfo(ACCOUNT_ID, labelMap))
        .thenReturn(Optional.of(harnessServiceInfo));
    ItemProcessor<PublishedMessage, EnrichedEvent<K8sWatchEvent>> enricher =
        k8sWatchEventConfig.enricher(workloadRepository, k8sLabelServiceInfoFetcher);
    PublishedMessage message = PublishedMessage.builder()
                                   .accountId(ACCOUNT_ID)
                                   .message(K8sWatchEvent.newBuilder()
                                                .setType(K8sWatchEvent.Type.TYPE_ADDED)
                                                .setClusterId(CLUSTER_ID)
                                                .setResourceRef(K8sObjectReference.newBuilder().setUid(UID).build())
                                                .setNewResourceVersion("12345")
                                                .setNewResourceYaml("yaml1")
                                                .build())
                                   .build();
    EnrichedEvent<K8sWatchEvent> enriched = enricher.process(message);
    assertThat(enriched.getHarnessServiceInfo()).isEqualTo(harnessServiceInfo);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWriter_shouldHandleAdded() throws Exception {
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    when(costEventService.create(captor.capture())).thenReturn(true);
    when(k8sYamlDao.ensureYamlSaved(ACCOUNT_ID, CLUSTER_ID, UID, "12345", "added-yaml"))
        .thenReturn("ZS5mUwfQSFO1pTljLKWG-Q");
    when(watchEventCostEstimator.estimateCost(any())).thenReturn(BigDecimal.valueOf(12.34));
    ItemWriter<EnrichedEvent<K8sWatchEvent>> writer =
        k8sWatchEventConfig.writer(k8sYamlDao, costEventService, watchEventCostEstimator);
    long occurredAt = 1583395677;
    K8sWatchEvent event = K8sWatchEvent.newBuilder()
                              .setType(K8sWatchEvent.Type.TYPE_ADDED)
                              .setDescription("Resource added")
                              .setClusterId(CLUSTER_ID)
                              .setClusterName(CLUSTER_NAME)
                              .setCloudProviderId(CLOUD_PROVIDER_ID)
                              .setResourceRef(K8sObjectReference.newBuilder().setUid(UID).build())
                              .setNewResourceYaml("added-yaml")
                              .setNewResourceVersion("12345")
                              .build();
    HarnessServiceInfo harnessServiceInfo = new HarnessServiceInfo(
        "svc-id", "app-id", "cloud-provider-id", "env-id", "infra-mapping-id", "deployment-summary-id");
    writer.write(singletonList(new EnrichedEvent<>(ACCOUNT_ID, occurredAt, event, harnessServiceInfo)));
    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().get(0)).isInstanceOfSatisfying(CostEventData.class, costEventData -> {
      assertThat(costEventData)
          .isEqualTo(CostEventData.builder()
                         .accountId(ACCOUNT_ID)
                         .clusterType(ClusterType.K8S.name())
                         .costEventSource(CostEventSource.K8S_CLUSTER.name())
                         .costEventType(CostEventType.K8S_RESOURCE_CHANGE.name())
                         .clusterId(CLUSTER_ID)
                         .startTimestamp(occurredAt)
                         .eventDescription("Resource added")
                         .newYamlRef("ZS5mUwfQSFO1pTljLKWG-Q")
                         .clusterId(CLUSTER_ID)
                         .appId("app-id")
                         .serviceId("svc-id")
                         .cloudProviderId("cloud-provider-id")
                         .envId("env-id")
                         .billingAmount(BigDecimal.valueOf(12.34))
                         .build());
    });
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWriter_shouldHandleUpdated() throws Exception {
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    when(costEventService.create(captor.capture())).thenReturn(true);
    when(k8sYamlDao.ensureYamlSaved(ACCOUNT_ID, CLUSTER_ID, UID, "12334", "old-yaml"))
        .thenReturn("Cb3cDTt3RBegwMzuV9gH3A");
    when(k8sYamlDao.ensureYamlSaved(ACCOUNT_ID, CLUSTER_ID, UID, "12345", "new-yaml"))
        .thenReturn("ZS5mUwfQSFO1pTljLKWG-Q");
    when(watchEventCostEstimator.estimateCost(any())).thenReturn(BigDecimal.valueOf(12.34));
    ItemWriter<EnrichedEvent<K8sWatchEvent>> writer =
        k8sWatchEventConfig.writer(k8sYamlDao, costEventService, watchEventCostEstimator);
    long occurredAt = 1583395677;
    K8sWatchEvent event = K8sWatchEvent.newBuilder()
                              .setType(K8sWatchEvent.Type.TYPE_UPDATED)
                              .setDescription("Resource updated")
                              .setClusterId(CLUSTER_ID)
                              .setClusterName(CLUSTER_NAME)
                              .setCloudProviderId(CLOUD_PROVIDER_ID)
                              .setResourceRef(K8sObjectReference.newBuilder().setUid(UID).build())
                              .setOldResourceYaml("old-yaml")
                              .setOldResourceVersion("12334")
                              .setNewResourceYaml("new-yaml")
                              .setNewResourceVersion("12345")
                              .build();
    HarnessServiceInfo harnessServiceInfo = new HarnessServiceInfo(
        "svc-id", "app-id", "cloud-provider-id", "env-id", "infra-mapping-id", "deployment-summary-id");
    writer.write(singletonList(new EnrichedEvent<>(ACCOUNT_ID, occurredAt, event, harnessServiceInfo)));
    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().get(0)).isInstanceOfSatisfying(CostEventData.class, costEventData -> {
      assertThat(costEventData)
          .isEqualTo(CostEventData.builder()
                         .accountId(ACCOUNT_ID)
                         .clusterType(ClusterType.K8S.name())
                         .costEventSource(CostEventSource.K8S_CLUSTER.name())
                         .costEventType(CostEventType.K8S_RESOURCE_CHANGE.name())
                         .clusterId(CLUSTER_ID)
                         .startTimestamp(occurredAt)
                         .eventDescription("Resource updated")
                         .oldYamlRef("Cb3cDTt3RBegwMzuV9gH3A")
                         .newYamlRef("ZS5mUwfQSFO1pTljLKWG-Q")
                         .clusterId(CLUSTER_ID)
                         .appId("app-id")
                         .serviceId("svc-id")
                         .cloudProviderId("cloud-provider-id")
                         .billingAmount(BigDecimal.valueOf(12.34))
                         .envId("env-id")
                         .build());
    });
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWriter_shouldHandleDeleted() throws Exception {
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    when(costEventService.create(captor.capture())).thenReturn(true);
    when(k8sYamlDao.ensureYamlSaved(ACCOUNT_ID, CLUSTER_ID, UID, "12334", "deleted-yaml"))
        .thenReturn("Cb3cDTt3RBegwMzuV9gH3A");
    when(watchEventCostEstimator.estimateCost(any())).thenReturn(BigDecimal.valueOf(12.34));
    ItemWriter<EnrichedEvent<K8sWatchEvent>> writer =
        k8sWatchEventConfig.writer(k8sYamlDao, costEventService, watchEventCostEstimator);
    long occurredAt = 1583395677;
    K8sWatchEvent event = K8sWatchEvent.newBuilder()
                              .setType(K8sWatchEvent.Type.TYPE_DELETED)
                              .setDescription("Resource deleted")
                              .setClusterId(CLUSTER_ID)
                              .setClusterName(CLUSTER_NAME)
                              .setCloudProviderId(CLOUD_PROVIDER_ID)
                              .setResourceRef(K8sObjectReference.newBuilder().setUid(UID).build())
                              .setOldResourceYaml("deleted-yaml")
                              .setOldResourceVersion("12334")
                              .build();
    HarnessServiceInfo harnessServiceInfo = new HarnessServiceInfo(
        "svc-id", "app-id", "cloud-provider-id", "env-id", "infra-mapping-id", "deployment-summary-id");
    writer.write(singletonList(new EnrichedEvent<>(ACCOUNT_ID, occurredAt, event, harnessServiceInfo)));
    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().get(0)).isInstanceOfSatisfying(CostEventData.class, costEventData -> {
      assertThat(costEventData)
          .isEqualTo(CostEventData.builder()
                         .accountId(ACCOUNT_ID)
                         .clusterType(ClusterType.K8S.name())
                         .costEventSource(CostEventSource.K8S_CLUSTER.name())
                         .costEventType(CostEventType.K8S_RESOURCE_CHANGE.name())
                         .clusterId(CLUSTER_ID)
                         .startTimestamp(occurredAt)
                         .eventDescription("Resource deleted")
                         .oldYamlRef("Cb3cDTt3RBegwMzuV9gH3A")
                         .clusterId(CLUSTER_ID)
                         .appId("app-id")
                         .serviceId("svc-id")
                         .cloudProviderId("cloud-provider-id")
                         .envId("env-id")
                         .billingAmount(BigDecimal.valueOf(12.34))
                         .build());
    });
  }
}
