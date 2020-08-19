package io.harness.perpetualtask.k8s.watch;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static io.harness.ccm.health.HealthStatusService.CLUSTER_ID_IDENTIFIER;
import static io.harness.grpc.utils.HTimestamps.toInstant;
import static io.harness.grpc.utils.HTimestamps.toMillis;
import static io.harness.perpetualtask.k8s.watch.PodEvent.EventType.EVENT_TYPE_SCHEDULED;
import static io.harness.perpetualtask.k8s.watch.PodEvent.EventType.EVENT_TYPE_TERMINATED;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.UTSAV;
import static io.kubernetes.client.custom.Quantity.Format.BINARY_SI;
import static io.kubernetes.client.custom.Quantity.Format.DECIMAL_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;

import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.rule.Owner;
import io.kubernetes.client.informer.EventType;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1ContainerBuilder;
import io.kubernetes.client.openapi.models.V1ListMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import io.kubernetes.client.openapi.models.V1PodConditionBuilder;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1VolumeBuilder;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class PodWatcherTest extends CategoryTest {
  private PodWatcher podWatcher;
  private EventPublisher eventPublisher;
  private SharedInformerFactory sharedInformerFactory;

  final DateTime TIMESTAMP = DateTime.now();
  final DateTime DELETION_TIMESTAMP = TIMESTAMP.plusMinutes(5);
  private static final String START_RV = "1001";
  private static final String END_RV = "1002";
  private static final UrlMatchingStrategy POD_URL_MATCHING = urlMatching("^/api/v1/pods.*");

  ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
  @Captor ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

  @Rule public WireMockRule wireMockRule = new WireMockRule(65222);

  @Before
  public void setUp() throws Exception {
    sharedInformerFactory = new SharedInformerFactory();
    eventPublisher = mock(EventPublisher.class);
    MockitoAnnotations.initMocks(this);
    K8sControllerFetcher controllerFetcher = mock(K8sControllerFetcher.class);

    when(controllerFetcher.getTopLevelOwner(any()))
        .thenReturn(io.harness.perpetualtask.k8s.watch.Owner.newBuilder()
                        .setKind("Deployment")
                        .setName("manager")
                        .setUid("9a1e372f-a7c1-410b-8b07-e09b0b965fcc")
                        .putLabels("app", "manager")
                        .putLabels("harness.io/release-name", "2cb07f52-ee19-3ab3-a3e7-8b8de3e2d0d1")
                        .build());

    podWatcher = new PodWatcher(new ClientBuilder().setBasePath("http://localhost:" + wireMockRule.port()).build(),
        ClusterDetails.builder()
            .clusterName("clusterName")
            .clusterId("clusterId")
            .cloudProviderId("cloud-provider-id")
            .kubeSystemUid("cluster-uid")
            .build(),
        controllerFetcher, sharedInformerFactory, null, eventPublisher);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  @Ignore("flaky test: comment this while testing on local")
  public void testEventFiredOnAdd() throws InterruptedException {
    V1PodList podList = new V1PodList().metadata(new V1ListMeta().resourceVersion(START_RV)).items(Arrays.asList());

    stubFor(get(POD_URL_MATCHING)
                .inScenario("onAdd")
                .willSetStateTo("watch=true")
                .withQueryParam("watch", equalTo("false"))
                .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(new JSON().serialize(podList))));

    V1Pod POD = podBuilder().build();
    POD.metadata(POD.getMetadata().resourceVersion(END_RV));
    Watch.Response<V1Pod> watchResponse = new Watch.Response<>(EventType.ADDED.name(), POD);

    stubFor(get(POD_URL_MATCHING)
                .inScenario("onAdd")
                .whenScenarioStateIs("watch=true")
                .willSetStateTo("random123")
                .withQueryParam("watch", equalTo("true"))
                .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(new JSON().serialize(watchResponse))));

    sharedInformerFactory.startAllRegisteredInformers();
    // try increase sleep if no requests received
    Thread.sleep(200);

    WireMock.verify(1, getRequestedFor(POD_URL_MATCHING).withQueryParam("watch", equalTo("false")));
    WireMock.verify(1, getRequestedFor(POD_URL_MATCHING).withQueryParam("watch", equalTo("true")));

    verify(eventPublisher, times(2)).publishMessage(captor.capture(), any(), any());

    assertThat(captor.getAllValues().get(0)).isInstanceOfSatisfying(PodInfo.class, this ::infoMessageAssertions);
    assertThat(captor.getAllValues().get(1)).isInstanceOfSatisfying(PodEvent.class, this ::scheduledMessageAssertions);

    sharedInformerFactory.stopAllRegisteredInformers();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPublishPodScheduledAndPodInfo() throws Exception {
    podWatcher.eventReceived(scheduledPod());
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
    podWatcher.eventReceived(scheduledAndDeletedPod());

    verify(eventPublisher, atLeastOnce())
        .publishMessage(captor.capture(), any(Timestamp.class), mapArgumentCaptor.capture());
    assertThat(captor.getAllValues().get(2)).isInstanceOfSatisfying(PodEvent.class, this ::deletedMessageAssertions);
    assertThat(mapArgumentCaptor.getValue().keySet()).contains(CLUSTER_ID_IDENTIFIER);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldNotPublishDuplicates() throws Exception {
    podWatcher.eventReceived(podBuilder().build()); // none
    podWatcher.eventReceived(scheduledPod()); // info, scheduled
    podWatcher.eventReceived(scheduledPod()); // none
    podWatcher.eventReceived(scheduledAndDeletedPod()); // deleted

    verify(eventPublisher, atLeastOnce())
        .publishMessage(captor.capture(), any(Timestamp.class), mapArgumentCaptor.capture());
    List<Message> publishedMessages = captor.getAllValues();
    assertThat(publishedMessages).hasSize(3);
    assertThat(publishedMessages.get(0)).isInstanceOfSatisfying(PodInfo.class, this ::infoMessageAssertions);
    assertThat(publishedMessages.get(1)).isInstanceOfSatisfying(PodEvent.class, this ::scheduledMessageAssertions);
    assertThat(publishedMessages.get(2)).isInstanceOfSatisfying(PodEvent.class, this ::deletedMessageAssertions);
    assertThat(mapArgumentCaptor.getValue().keySet()).contains(CLUSTER_ID_IDENTIFIER);
  }

  private V1Pod scheduledPod() {
    return podBuilder()
        .editSpec()
        .withNodeName("gke-pr-private-pool-1-49d0f375-12xx")
        .endSpec()
        .editOrNewStatus()
        .withConditions(new V1PodConditionBuilder()
                            .withLastTransitionTime(TIMESTAMP)
                            .withType("PodScheduled")
                            .withStatus("True")
                            .build())
        .endStatus()
        .build();
  }

  private V1Pod scheduledAndDeletedPod() {
    return new V1PodBuilder(scheduledPod())
        .editMetadata()
        .withDeletionGracePeriodSeconds(0L)
        .withDeletionTimestamp(DELETION_TIMESTAMP)
        .endMetadata()
        .build();
  }

  private V1PodBuilder podBuilder() {
    return new V1PodBuilder()
        .withApiVersion("v1")
        .withNewMetadata()
        .withUid("948e988d-d300-11e9-b63d-4201ac100a04")
        .withName("manager-79cc97bdfb-r6kzs")
        .withCreationTimestamp(TIMESTAMP)
        .withNamespace("harness")
        .withLabels(
            ImmutableMap.of("app", "manager", "harness.io/release-name", "2cb07f52-ee19-3ab3-a3e7-8b8de3e2d0d1"))
        .withResourceVersion("77330477")
        .endMetadata()
        .withNewStatus()
        .withConditions(ImmutableList.of(new V1PodConditionBuilder()
                                             .withType("PodScheduled")
                                             .withStatus("True")
                                             .withLastTransitionTime(TIMESTAMP)
                                             .build()))
        .withQosClass("Guaranteed")
        .endStatus()
        .withNewSpec()
        .withVolumes(new V1VolumeBuilder()
                         .withNewPersistentVolumeClaim()
                         .withClaimName("mongo-data")
                         .endPersistentVolumeClaim()
                         .build())
        .withNodeName("gke-pr-private-pool-1-49d0f375-12xx")
        .withContainers(
            new V1ContainerBuilder()
                .withImage("us.gcr.io/platform-205701/harness/feature-manager:19204")
                .withName("manager")
                .withNewResources()
                .addToLimits("cpu", new io.kubernetes.client.custom.Quantity(new BigDecimal("1"), DECIMAL_SI))
                .addToLimits(
                    "memory", new io.kubernetes.client.custom.Quantity(new BigDecimal("2861563904"), BINARY_SI))
                .addToRequests("cpu", new io.kubernetes.client.custom.Quantity(new BigDecimal("1"), DECIMAL_SI))
                .addToRequests(
                    "memory", new io.kubernetes.client.custom.Quantity(new BigDecimal("2861563904"), BINARY_SI))
                .endResources()
                .build())
        .endSpec();
  }

  private void deletedMessageAssertions(PodEvent podEvent) {
    assertThat(podEvent.getPodUid()).isEqualTo("948e988d-d300-11e9-b63d-4201ac100a04");
    assertThat(podEvent.getType()).isEqualTo(EVENT_TYPE_TERMINATED);
    assertThat(toInstant(podEvent.getTimestamp()).toEpochMilli()).isEqualTo(DELETION_TIMESTAMP.toInstant().getMillis());
  }

  private void scheduledMessageAssertions(PodEvent podEvent) {
    assertThat(podEvent.getPodUid()).isEqualTo("948e988d-d300-11e9-b63d-4201ac100a04");
    assertThat(podEvent.getType()).isEqualTo(EVENT_TYPE_SCHEDULED);
    assertThat(toMillis(podEvent.getTimestamp())).isEqualTo(TIMESTAMP.getMillis());
  }

  private void infoMessageAssertions(PodInfo podInfo) {
    assertThat(podInfo.getPodUid()).isEqualTo("948e988d-d300-11e9-b63d-4201ac100a04");
    assertThat(podInfo.getPodName()).isEqualTo("manager-79cc97bdfb-r6kzs");
    assertThat(toMillis(podInfo.getCreationTimestamp())).isEqualTo(TIMESTAMP.getMillis());
    assertThat(podInfo.getNamespace()).isEqualTo("harness");
    assertThat(podInfo.getNodeName()).isEqualTo("gke-pr-private-pool-1-49d0f375-12xx");
    assertThat(podInfo.getContainersList())
        .containsExactly(
            Container.newBuilder()
                .setName("manager")
                .setImage("us.gcr.io/platform-205701/harness/feature-manager:19204")
                .setResource(
                    Resource.newBuilder()
                        .putLimits("cpu", Quantity.newBuilder().setAmount(1_000_000_000L).setUnit("n").build())
                        .putLimits("memory", Quantity.newBuilder().setAmount(2861563904L).setUnit("").build())
                        .putRequests("cpu", Quantity.newBuilder().setAmount(1_000_000_000).setUnit("n").build())
                        .putRequests("memory", Quantity.newBuilder().setAmount(2861563904L).setUnit("").build())
                        .build())
                .build());
    assertThat(podInfo.getQosClass()).isEqualTo("Guaranteed");
    assertThat(podInfo.getTotalResource())
        .isEqualTo(Resource.newBuilder()
                       .putLimits("cpu", Quantity.newBuilder().setAmount(1_000_000_000).setUnit("n").build())
                       .putLimits("memory", Quantity.newBuilder().setAmount(2861563904L).setUnit("").build())
                       .putRequests("cpu", Quantity.newBuilder().setAmount(1_000_000_000).setUnit("n").build())
                       .putRequests("memory", Quantity.newBuilder().setAmount(2861563904L).setUnit("").build())
                       .build());
    assertThat(podInfo.getLabelsMap())
        .isEqualTo(
            ImmutableMap.of("app", "manager", "harness.io/release-name", "2cb07f52-ee19-3ab3-a3e7-8b8de3e2d0d1"));
    assertThat(podInfo.getTopLevelOwner())
        .isEqualTo(io.harness.perpetualtask.k8s.watch.Owner.newBuilder()
                       .setKind("Deployment")
                       .setName("manager")
                       .setUid("9a1e372f-a7c1-410b-8b07-e09b0b965fcc")
                       .putLabels("app", "manager")
                       .putLabels("harness.io/release-name", "2cb07f52-ee19-3ab3-a3e7-8b8de3e2d0d1")
                       .build());
  }
}
