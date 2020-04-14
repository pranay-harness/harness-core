package io.harness.perpetualtask.k8s.watch;

import static io.harness.ccm.health.HealthStatusService.CLUSTER_ID_IDENTIFIER;
import static io.harness.perpetualtask.k8s.watch.PodEvent.EventType.EVENT_TYPE_SCHEDULED;
import static io.harness.perpetualtask.k8s.watch.PodEvent.EventType.EVENT_TYPE_TERMINATED;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodConditionBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.fabric8.kubernetes.client.dsl.FilterWatchListMultiDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.client.EventPublisher;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.watch.Resource.Quantity;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

@Slf4j
public class PodWatcherTest extends CategoryTest {
  private PodWatcher podWatcher;
  private EventPublisher eventPublisher;
  private Watch watch;
  @Captor ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

  @Before
  public void setUp() throws Exception {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    eventPublisher = mock(EventPublisher.class);
    watch = mock(Watch.class);
    MockitoAnnotations.initMocks(this);
    @SuppressWarnings("unchecked")
    MixedOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> podeOps = mock(MixedOperation.class);
    @SuppressWarnings("unchecked")
    FilterWatchListMultiDeletable<Pod, PodList, Boolean, Watch, Watcher<Pod>> ks =
        mock(FilterWatchListMultiDeletable.class);
    when(kubernetesClient.pods()).thenReturn(podeOps);
    when(podeOps.inAnyNamespace()).thenReturn(ks);
    when(ks.watch(any())).thenReturn(watch);
    podWatcher = new PodWatcher(kubernetesClient,
        ClusterDetails.builder()
            .clusterName("clusterName")
            .clusterId("clusterId")
            .cloudProviderId("cloud-provider-id")
            .kubeSystemUid("cluster-uid")
            .build(),
        eventPublisher);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishPodScheduledAndPodInfo() throws Exception {
    podWatcher.eventReceived(Action.MODIFIED, scheduledPod());
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(eventPublisher, times(2))
        .publishMessage(captor.capture(), any(Timestamp.class), mapArgumentCaptor.capture());
    assertThat(captor.getAllValues()).hasSize(2);
    assertThat(captor.getAllValues().get(0)).isInstanceOfSatisfying(PodInfo.class, this ::infoMessageAssertions);
    assertThat(captor.getAllValues().get(1)).isInstanceOfSatisfying(PodEvent.class, this ::scheduledMessageAssertions);
    assertThat(mapArgumentCaptor.getValue().keySet()).contains(CLUSTER_ID_IDENTIFIER);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishPodDeleted() throws Exception {
    podWatcher.eventReceived(Action.DELETED, scheduledAndDeletedPod());
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(eventPublisher, atLeastOnce())
        .publishMessage(captor.capture(), any(Timestamp.class), mapArgumentCaptor.capture());
    assertThat(captor.getAllValues().get(2)).isInstanceOfSatisfying(PodEvent.class, this ::deletedMessageAssertions);
    assertThat(mapArgumentCaptor.getValue().keySet()).contains(CLUSTER_ID_IDENTIFIER);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldNotPublishDuplicates() throws Exception {
    podWatcher.eventReceived(Action.ADDED, podBuilder().build()); // none
    podWatcher.eventReceived(Action.MODIFIED, scheduledPod()); // info, scheduled
    podWatcher.eventReceived(Action.MODIFIED, scheduledPod()); // none
    podWatcher.eventReceived(Action.DELETED, scheduledAndDeletedPod()); // deleted
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(eventPublisher, atLeastOnce())
        .publishMessage(captor.capture(), any(Timestamp.class), mapArgumentCaptor.capture());
    List<Message> publishedMessages = captor.getAllValues();
    assertThat(publishedMessages).hasSize(3);
    assertThat(publishedMessages.get(0)).isInstanceOfSatisfying(PodInfo.class, this ::infoMessageAssertions);
    assertThat(publishedMessages.get(1)).isInstanceOfSatisfying(PodEvent.class, this ::scheduledMessageAssertions);
    assertThat(publishedMessages.get(2)).isInstanceOfSatisfying(PodEvent.class, this ::deletedMessageAssertions);
    assertThat(mapArgumentCaptor.getValue().keySet()).contains(CLUSTER_ID_IDENTIFIER);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldCloseUnderlyingWatchOnClosingWatcher() throws Exception {
    podWatcher.onClose(null);
    verify(watch).close();
  }

  private Pod scheduledPod() {
    return podBuilder()
        .editSpec()
        .withNodeName("gke-pr-private-pool-1-49d0f375-12xx")
        .endSpec()
        .withNewStatus()
        .withConditions(new PodConditionBuilder()
                            .withLastTransitionTime("2019-09-09T18:21:45.000+05:30")
                            .withType("PodScheduled")
                            .withStatus("True")
                            .build())
        .endStatus()
        .build();
  }

  private Pod scheduledAndDeletedPod() {
    return new PodBuilder(scheduledPod())
        .editMetadata()
        .withDeletionGracePeriodSeconds(0L)
        .withDeletionTimestamp("2019-09-09T19:34:33.000+05:30")
        .endMetadata()
        .build();
  }

  private PodBuilder podBuilder() {
    return new PodBuilder()
        .withApiVersion("v1")
        .withNewMetadata()
        .withUid("948e988d-d300-11e9-b63d-4201ac100a04")
        .withName("manager-79cc97bdfb-r6kzs")
        .withCreationTimestamp("2019-09-09T18:21:45.000+05:30")
        .withNamespace("harness")
        .withLabels(
            ImmutableMap.of("app", "manager", "harness.io/release-name", "2cb07f52-ee19-3ab3-a3e7-8b8de3e2d0d1"))
        .withResourceVersion("77330477")
        .endMetadata()
        .withNewStatus()
        .endStatus()
        .withNewSpec()
        .withContainers(
            new ContainerBuilder()
                .withImage("us.gcr.io/platform-205701/harness/feature-manager:19204")
                .withName("manager")
                .withNewResources()
                .addToLimits("cpu", new QuantityBuilder().withAmount("1").withFormat("DECIMAL_SI").build())
                .addToLimits("memory", new QuantityBuilder().withAmount("2861563904").withFormat("BINARY_SI").build())
                .addToRequests("cpu", new QuantityBuilder().withAmount("1").withFormat("DECIMAL_SI").build())
                .addToRequests("memory", new QuantityBuilder().withAmount("2861563904").withFormat("BINARY_SI").build())
                .endResources()
                .build())
        .endSpec();
  }

  private void deletedMessageAssertions(PodEvent podEvent) {
    assertThat(podEvent.getPodUid()).isEqualTo("948e988d-d300-11e9-b63d-4201ac100a04");
    assertThat(podEvent.getType()).isEqualTo(EVENT_TYPE_TERMINATED);
    assertThat(podEvent.getTimestamp()).isEqualTo(HTimestamps.parse("2019-09-09T19:34:33.000+05:30"));
  }

  private void scheduledMessageAssertions(PodEvent podEvent) {
    assertThat(podEvent.getPodUid()).isEqualTo("948e988d-d300-11e9-b63d-4201ac100a04");
    assertThat(podEvent.getType()).isEqualTo(EVENT_TYPE_SCHEDULED);
    assertThat(podEvent.getTimestamp()).isEqualTo(HTimestamps.parse("2019-09-09T18:21:45.000+05:30"));
  }

  private void infoMessageAssertions(PodInfo podInfo) {
    assertThat(podInfo.getPodUid()).isEqualTo("948e988d-d300-11e9-b63d-4201ac100a04");
    assertThat(podInfo.getPodName()).isEqualTo("manager-79cc97bdfb-r6kzs");
    assertThat(podInfo.getCreationTimestamp()).isEqualTo(HTimestamps.parse("2019-09-09T18:21:45.000+05:30"));
    assertThat(podInfo.getNamespace()).isEqualTo("harness");
    assertThat(podInfo.getNodeName()).isEqualTo("gke-pr-private-pool-1-49d0f375-12xx");
    assertThat(podInfo.getContainersList())
        .containsExactly(
            Container.newBuilder()
                .setName("manager")
                .setImage("us.gcr.io/platform-205701/harness/feature-manager:19204")
                .setResource(
                    Resource.newBuilder()
                        .putLimits("cpu", Quantity.newBuilder().setAmount(1_000_000_000).setUnit("n").build())
                        .putLimits("memory", Quantity.newBuilder().setAmount(2861563904L).setUnit("").build())
                        .putRequests("cpu", Quantity.newBuilder().setAmount(1_000_000_000).setUnit("n").build())
                        .putRequests("memory", Quantity.newBuilder().setAmount(2861563904L).setUnit("").build())
                        .build())
                .build());
    assertThat(podInfo.getLabelsMap())
        .isEqualTo(
            ImmutableMap.of("app", "manager", "harness.io/release-name", "2cb07f52-ee19-3ab3-a3e7-8b8de3e2d0d1"));
  }
}
