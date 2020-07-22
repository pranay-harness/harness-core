package io.harness.batch.processing.config.k8s.recommendation;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec.ContainerSpec;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class WorkloadSpecWriterTest extends CategoryTest {
  private static final String ACCOUNT_ID = "ACCOUNT_ID1";
  public static final String CLUSTER_ID = "CLUSTER_ID1";

  @Mock private WorkloadRecommendationDao workloadRecommendationDao;
  @InjectMocks private WorkloadSpecWriter workloadSpecWriter;

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWrite() throws Exception {
    when(workloadRecommendationDao.fetchRecommendationForWorkload(any()))
        .thenReturn(K8sWorkloadRecommendation.builder()
                        .accountId(ACCOUNT_ID)
                        .clusterId(CLUSTER_ID)
                        .namespace("kube-system")
                        .workloadName("kube-dns")
                        .workloadType("Deployment")
                        .containerRecommendations(new HashMap<>())
                        .containerCheckpoints(new HashMap<>())
                        .build());
    workloadSpecWriter.write(messages());
    ArgumentCaptor<K8sWorkloadRecommendation> captor = ArgumentCaptor.forClass(K8sWorkloadRecommendation.class);
    verify(workloadRecommendationDao).save(captor.capture());
    assertThat(captor.getAllValues()).hasSize(1);
    Map<String, ContainerRecommendation> containerRecommendations = captor.getValue().getContainerRecommendations();
    assertThat(containerRecommendations).hasSize(5);
    assertThat(containerRecommendations.get("kubedns").getCurrent())
        .isEqualTo(ResourceRequirement.builder()
                       .request("cpu", "100m")
                       .request("memory", "70Mi")
                       .limit("memory", "170Mi")
                       .build());
    assertThat(containerRecommendations.get("dnsmasq").getCurrent())
        .isEqualTo(ResourceRequirement.builder().request("cpu", "150m").request("memory", "20Mi").build());
    assertThat(containerRecommendations.get("sidecar").getCurrent())
        .isEqualTo(ResourceRequirement.builder().request("cpu", "10m").request("memory", "20Mi").build());
    assertThat(containerRecommendations.get("without-requests").getCurrent())
        .isEqualTo(ResourceRequirement.builder()
                       .request("cpu", "10m")
                       .request("memory", "20Mi")
                       .limit("cpu", "10m")
                       .limit("memory", "20Mi")
                       .build());
    assertThat(containerRecommendations.get("nothing").getCurrent()).isEqualTo(ResourceRequirement.builder().build());
  }

  private List<? extends PublishedMessage> messages() {
    return ImmutableList.of(PublishedMessage.builder()
                                .accountId(ACCOUNT_ID)
                                .message(K8sWorkloadSpec.newBuilder()
                                             .setClusterId(CLUSTER_ID)
                                             .setNamespace("kube-system")
                                             .setWorkloadKind("kube-dns")
                                             .setWorkloadName("Deployment")
                                             .addContainerSpecs(ContainerSpec.newBuilder()
                                                                    .setName("kubedns")
                                                                    .putRequests("cpu", "100m")
                                                                    .putRequests("memory", "70Mi")
                                                                    .putLimits("memory", "170Mi")
                                                                    .build())

                                             .addContainerSpecs(ContainerSpec.newBuilder()
                                                                    .setName("dnsmasq")
                                                                    .putRequests("cpu", "150m")
                                                                    .putRequests("memory", "20Mi")
                                                                    .build())
                                             .addContainerSpecs(ContainerSpec.newBuilder()
                                                                    .setName("sidecar")
                                                                    .putRequests("cpu", "10m")
                                                                    .putRequests("memory", "20Mi")
                                                                    .build())

                                             .addContainerSpecs(ContainerSpec.newBuilder()
                                                                    .setName("without-requests")
                                                                    .putLimits("cpu", "10m")
                                                                    .putLimits("memory", "20Mi")
                                                                    .build())

                                             .addContainerSpecs(ContainerSpec.newBuilder().setName("nothing").build())
                                             .build())
                                .build());
  }
}
