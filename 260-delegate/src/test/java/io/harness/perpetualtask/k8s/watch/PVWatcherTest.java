package io.harness.perpetualtask.k8s.watch;

import static io.harness.perpetualtask.k8s.watch.PVInfo.PVType.PV_TYPE_GCE_PERSISTENT_DISK;
import static io.harness.rule.OwnerRule.UTSAV;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.client.EventPublisher;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.rule.Owner;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeBuilder;
import io.kubernetes.client.openapi.models.V1StorageClass;
import io.kubernetes.client.openapi.models.V1StorageClassBuilder;
import io.kubernetes.client.util.ClientBuilder;
import java.util.Collections;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

public class PVWatcherTest extends CategoryTest {
  private PVWatcher pvWatcher;
  private EventPublisher eventPublisher;
  private V1PersistentVolume samplePV;
  private PVInfo pvInfo;
  private PVEvent pvEvent;
  private ClusterDetails clusterDetails;

  final DateTime TIMESTAMP = DateTime.now();
  final DateTime DELETION_TIMESTAMP = TIMESTAMP.plusMinutes(5);
  private static final String DEFAULT_STORAGE_CLASS_TYPE = "default";
  private static final String SC_URL = "^/apis/storage.k8s.io/v1/storageclasses/";

  ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
  @Captor ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

  @Rule public WireMockRule wireMockRule = new WireMockRule(65225);

  @Before
  public void setUp() throws Exception {
    eventPublisher = mock(EventPublisher.class);
    MockitoAnnotations.initMocks(this);
    clusterDetails = ClusterDetails.builder()
                         .clusterName("clusterName")
                         .clusterId("clusterId")
                         .cloudProviderId("cloud-provider-id")
                         .kubeSystemUid("cluster-uid")
                         .build();
    pvWatcher = new PVWatcher(new ClientBuilder().setBasePath("http://localhost:" + wireMockRule.port()).build(),
        clusterDetails, new SharedInformerFactory(), eventPublisher);

    samplePV = new V1PersistentVolumeBuilder()
                   .withNewMetadata()
                   .withUid("uid")
                   .withName("test-pv")
                   .withCreationTimestamp(TIMESTAMP)
                   .endMetadata()
                   .withNewSpec()
                   .withNewGcePersistentDisk()
                   .withFsType("fsType")
                   .withPdName("gke-pr-private-0353f5d-pvc-ffefcf96-b30b-430b-9901-e5d8f6f636fe")
                   .endGcePersistentDisk()
                   .withCapacity(ImmutableMap.of("storage", new Quantity("10Ki")))
                   .withStorageClassName("standard")
                   .withNewClaimRef()
                   .withName("mongo-data")
                   .withNamespace("harness-namespace")
                   .endClaimRef()
                   .endSpec()
                   .build();

    pvEvent = PVEvent.newBuilder()
                  .setCloudProviderId(clusterDetails.getCloudProviderId())
                  .setClusterId(clusterDetails.getClusterId())
                  .setKubeSystemUid(clusterDetails.getKubeSystemUid())
                  .setTimestamp(HTimestamps.fromMillis(TIMESTAMP.getMillis()))
                  .setPvName(samplePV.getMetadata().getName())
                  .setPvUid(samplePV.getMetadata().getUid())
                  .build();

    pvInfo = PVInfo.newBuilder()
                 .setClaimNamespace(samplePV.getSpec().getClaimRef().getNamespace())
                 .setClaimName(samplePV.getSpec().getClaimRef().getName())
                 .setPvType(PV_TYPE_GCE_PERSISTENT_DISK)
                 .setCreationTimestamp(HTimestamps.fromMillis(TIMESTAMP.getMillis()))
                 .setCapacity(K8sResourceUtils.getStorageCapacity(samplePV.getSpec()))
                 .setPvUid(samplePV.getMetadata().getUid())
                 .setPvName(samplePV.getMetadata().getName())
                 .putAllLabels(Collections.emptyMap())
                 .setCapacity(K8sResourceUtils.getStorageCapacity(samplePV.getSpec()))
                 .setStorageClassType(DEFAULT_STORAGE_CLASS_TYPE)
                 .setClusterId(clusterDetails.getClusterId())
                 .setClusterName(clusterDetails.getClusterName())
                 .setCloudProviderId(clusterDetails.getCloudProviderId())
                 .setKubeSystemUid(clusterDetails.getKubeSystemUid())
                 .build();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldPublishOnAdd() throws Exception {
    pvWatcher.onAdd(samplePV);

    verify(eventPublisher, times(1))
        .publishMessage(captor.capture(), any(Timestamp.class), mapArgumentCaptor.capture());

    WireMock.verify(1, getRequestedFor(urlMatching(SC_URL + samplePV.getSpec().getStorageClassName() + ".*")));

    assertThat(captor.getAllValues().get(0)).isEqualTo(pvInfo);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testPublishOnVolumeExpansion() throws Exception {
    V1PersistentVolume oldPV = new V1PersistentVolumeBuilder()
                                   .withNewMetadataLike(samplePV.getMetadata())
                                   .endMetadata()
                                   .withNewSpecLike(samplePV.getSpec())
                                   .endSpec()
                                   .build();
    oldPV.getSpec().setCapacity(ImmutableMap.of("storage", new Quantity("9Ki")));

    pvWatcher.onUpdate(oldPV, samplePV);

    verify(eventPublisher, times(2))
        .publishMessage(captor.capture(), any(Timestamp.class), mapArgumentCaptor.capture());

    WireMock.verify(1, getRequestedFor(urlMatching(SC_URL + samplePV.getSpec().getStorageClassName() + ".*")));

    System.out.println(K8sResourceUtils.getStorageCapacity(samplePV.getSpec()));
    assertThat(captor.getAllValues().get(0)).isEqualTo(pvInfo);

    assertThat(captor.getAllValues().get(1).toString())
        .contains(pvEvent.getPvName())
        .contains(pvEvent.getPvUid())
        .contains(PVEvent.EventType.EVENT_TYPE_EXPANSION.name());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testPublishOnDelete() throws Exception {
    samplePV.getMetadata().setDeletionTimestamp(DELETION_TIMESTAMP);
    pvWatcher.onDelete(samplePV, false);

    verify(eventPublisher, times(1))
        .publishMessage(captor.capture(), any(Timestamp.class), mapArgumentCaptor.capture());

    verify(eventPublisher, times(1))
        .publishMessage(captor.capture(), any(Timestamp.class), mapArgumentCaptor.capture());

    WireMock.verify(0, getRequestedFor(urlMatching(SC_URL + samplePV.getSpec().getStorageClassName() + ".*")));

    assertThat(captor.getAllValues().get(0))
        .isEqualTo(PVEvent.newBuilder(pvEvent)
                       .setEventType(PVEvent.EventType.EVENT_TYPE_STOP)
                       .setTimestamp(HTimestamps.fromMillis(DELETION_TIMESTAMP.getMillis()))
                       .build());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetStorageType() throws Exception {
    V1StorageClass storageClass = new V1StorageClassBuilder().withParameters(ImmutableMap.of("type", "pd-ssd")).build();

    stubFor(get(urlMatching(SC_URL + "standard.*"))
                .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(new JSON().serialize(storageClass))));

    assertThat(pvWatcher.getStorageType(samplePV)).isEqualTo("pd-ssd");
    WireMock.verify(1, getRequestedFor(urlMatching(SC_URL + "standard.*")));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetStorageTypeWithNullStorageType() throws Exception {
    V1StorageClass storageClass = new V1StorageClassBuilder().withParameters(ImmutableMap.of()).build();

    stubFor(get(urlMatching(SC_URL + "standard.*"))
                .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(new JSON().serialize(storageClass))));

    assertThat(pvWatcher.getStorageType(samplePV)).isEqualTo(DEFAULT_STORAGE_CLASS_TYPE);
    WireMock.verify(1, getRequestedFor(urlMatching(SC_URL + "standard.*")));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetStorageTypeWithNullParams() throws Exception {
    V1StorageClass storageClass = new V1StorageClassBuilder().build();

    stubFor(get(urlMatching(SC_URL + "standard.*"))
                .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(new JSON().serialize(storageClass))));

    assertThat(pvWatcher.getStorageType(samplePV)).isEqualTo(DEFAULT_STORAGE_CLASS_TYPE);
    WireMock.verify(1, getRequestedFor(urlMatching(SC_URL + "standard.*")));
  }
}
