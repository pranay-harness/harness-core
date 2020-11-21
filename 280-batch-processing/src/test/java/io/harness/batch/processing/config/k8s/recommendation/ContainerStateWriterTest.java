package io.harness.batch.processing.config.k8s.recommendation;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.entities.InstanceData;
import io.harness.ccm.recommender.k8sworkload.RecommenderUtils;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.ContainerStateProto;
import io.harness.grpc.utils.HTimestamps;
import io.harness.rule.Owner;

import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerCheckpoint;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;

import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ContainerStateWriterTest extends CategoryTest {
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  public static final String CLUSTER_ID = "CLUSTER_ID";
  public static final String NAMESPACE = "NAMESPACE";
  public static final String POD_NAME = "POD_NAME";
  public static final String WORKLOAD_NAME = "WORKLOAD_NAME";
  public static final String WORKLOAD_TYPE = "WORKLOAD_TYPE";
  public static final String CONTAINER_NAME = "CONTAINER_NAME";
  public static final Instant anyTime = Instant.EPOCH;
  @InjectMocks private ContainerStateWriter containerStateWriter;

  @Mock private InstanceDataDao instanceDataDao;
  @Mock private WorkloadRecommendationDao workloadRecommendationDao;

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWriteSkipVersion() throws Exception {
    containerStateWriter.write(
        Collections.singletonList(PublishedMessage.builder()
                                      .accountId(ACCOUNT_ID)
                                      .message(ContainerStateProto.newBuilder().setVersion(0).build())
                                      .build()));
    verifyZeroInteractions(instanceDataDao);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testPodToWorkloadMappingNotFound() throws Exception {
    containerStateWriter.write(Collections.singletonList(PublishedMessage.builder()
                                                             .accountId(ACCOUNT_ID)
                                                             .message(ContainerStateProto.newBuilder()
                                                                          .setVersion(1)
                                                                          .setClusterId(CLUSTER_ID)
                                                                          .setNamespace(NAMESPACE)
                                                                          .setPodName(POD_NAME)
                                                                          .build())
                                                             .build()));
    verify(instanceDataDao).getK8sPodInstance(ACCOUNT_ID, CLUSTER_ID, NAMESPACE, POD_NAME);
    verifyZeroInteractions(workloadRecommendationDao);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testFetchWorkloadIdForPodNotFound() throws Exception {
    assertThat(containerStateWriter.fetchWorkloadIdForPod(ResourceId.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .clusterId(CLUSTER_ID)
                                                              .namespace(NAMESPACE)
                                                              .name(POD_NAME)
                                                              .kind("Pod")
                                                              .build()))
        .isSameAs(ResourceId.NOT_FOUND);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testFetchWorkloadIdForPod() throws Exception {
    when(instanceDataDao.getK8sPodInstance(ACCOUNT_ID, CLUSTER_ID, NAMESPACE, POD_NAME))
        .thenReturn(InstanceData.builder()
                        .metaData(ImmutableMap.of(InstanceMetaDataConstants.WORKLOAD_NAME, WORKLOAD_NAME,
                            InstanceMetaDataConstants.WORKLOAD_TYPE, WORKLOAD_TYPE))
                        .build());
    assertThat(containerStateWriter.fetchWorkloadIdForPod(ResourceId.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .clusterId(CLUSTER_ID)
                                                              .namespace(NAMESPACE)
                                                              .name(POD_NAME)
                                                              .kind("Pod")
                                                              .build()))
        .isEqualTo(ResourceId.builder()
                       .accountId(ACCOUNT_ID)
                       .clusterId(CLUSTER_ID)
                       .namespace(NAMESPACE)
                       .name(WORKLOAD_NAME)
                       .kind(WORKLOAD_TYPE)
                       .build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWriteIntervalCoveredAlready() throws Exception {
    when(instanceDataDao.getK8sPodInstance(ACCOUNT_ID, CLUSTER_ID, NAMESPACE, POD_NAME))
        .thenReturn(InstanceData.builder()
                        .metaData(ImmutableMap.of(InstanceMetaDataConstants.WORKLOAD_NAME, WORKLOAD_NAME,
                            InstanceMetaDataConstants.WORKLOAD_TYPE, WORKLOAD_TYPE))
                        .build());
    K8sWorkloadRecommendation originalRecommendation =
        K8sWorkloadRecommendation.builder()
            .accountId(ACCOUNT_ID)
            .clusterId(CLUSTER_ID)
            .namespace(NAMESPACE)
            .workloadName(WORKLOAD_NAME)
            .workloadType(WORKLOAD_TYPE)
            .containerCheckpoint(CONTAINER_NAME,
                ContainerCheckpoint.builder()
                    .version(1)
                    .cpuHistogram(RecommenderUtils.newCpuHistogram().saveToCheckpoint())
                    .memoryHistogram(RecommenderUtils.newMemoryHistogram().saveToCheckpoint())
                    // original recommendation already has data till 50th minute
                    .firstSampleStart(anyTime.plus(Duration.ofMinutes(10)))
                    .lastSampleStart(anyTime.plus(Duration.ofMinutes(50)))
                    .build())
            .build();
    when(workloadRecommendationDao.fetchRecommendationForWorkload(ResourceId.builder()
                                                                      .accountId(ACCOUNT_ID)
                                                                      .clusterId(CLUSTER_ID)
                                                                      .namespace(NAMESPACE)
                                                                      .name(WORKLOAD_NAME)
                                                                      .kind(WORKLOAD_TYPE)
                                                                      .build()))
        .thenReturn(originalRecommendation);
    containerStateWriter.write(Collections.singletonList(
        PublishedMessage.builder()
            .accountId(ACCOUNT_ID)
            .message(ContainerStateProto.newBuilder()
                         .setVersion(1)
                         .setClusterId(CLUSTER_ID)
                         .setNamespace(NAMESPACE)
                         .setPodName(POD_NAME)
                         .setContainerName(CONTAINER_NAME)
                         // new message for 40th-60th minute => should be rejected,
                         // as firstSampleStart(msg) > lastSampleStart(recommendation)
                         .setFirstSampleStart(HTimestamps.fromInstant(anyTime.plus(Duration.ofMinutes(40))))
                         .setLastSampleStart(HTimestamps.fromInstant(anyTime.plus(Duration.ofMinutes(60))))
                         .setTotalSamplesCount(20)
                         .build())
            .build()));
    ArgumentCaptor<K8sWorkloadRecommendation> captor = ArgumentCaptor.forClass(K8sWorkloadRecommendation.class);
    verify(workloadRecommendationDao).save(captor.capture());
    assertThat(captor.getAllValues()).hasSize(1);

    // no change in the recommendation as the message is skipped
    assertThat(captor.getValue()).isEqualTo(originalRecommendation);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWriteAccepted() throws Exception {
    when(instanceDataDao.getK8sPodInstance(ACCOUNT_ID, CLUSTER_ID, NAMESPACE, POD_NAME))
        .thenReturn(InstanceData.builder()
                        .metaData(ImmutableMap.of(InstanceMetaDataConstants.WORKLOAD_NAME, WORKLOAD_NAME,
                            InstanceMetaDataConstants.WORKLOAD_TYPE, WORKLOAD_TYPE))
                        .build());
    int originalSamplesCount = 10;
    Instant originalLastSampleStart = anyTime.plus(Duration.ofMinutes(50));
    K8sWorkloadRecommendation originalRecommendation =
        K8sWorkloadRecommendation.builder()
            .accountId(ACCOUNT_ID)
            .clusterId(CLUSTER_ID)
            .namespace(NAMESPACE)
            .workloadName(WORKLOAD_NAME)
            .workloadType(WORKLOAD_TYPE)
            .containerCheckpoint(CONTAINER_NAME,
                ContainerCheckpoint.builder()
                    .version(1)
                    .cpuHistogram(RecommenderUtils.newCpuHistogram().saveToCheckpoint())
                    .memoryHistogram(RecommenderUtils.newMemoryHistogram().saveToCheckpoint())
                    // original recommendation already has data till 50th minute
                    .firstSampleStart(anyTime.plus(Duration.ofMinutes(10)))
                    .lastSampleStart(originalLastSampleStart)
                    .totalSamplesCount(originalSamplesCount)
                    .build())
            .build();
    when(workloadRecommendationDao.fetchRecommendationForWorkload(ResourceId.builder()
                                                                      .accountId(ACCOUNT_ID)
                                                                      .clusterId(CLUSTER_ID)
                                                                      .namespace(NAMESPACE)
                                                                      .name(WORKLOAD_NAME)
                                                                      .kind(WORKLOAD_TYPE)
                                                                      .build()

                 ))
        .thenReturn(originalRecommendation);
    int msgSamplesCount = 20;
    Instant msgLastSampleStart = anyTime.plus(Duration.ofMinutes(70));
    containerStateWriter.write(Collections.singletonList(
        PublishedMessage.builder()
            .accountId(ACCOUNT_ID)
            .message(ContainerStateProto.newBuilder()
                         .setVersion(1)
                         .setClusterId(CLUSTER_ID)
                         .setNamespace(NAMESPACE)
                         .setPodName(POD_NAME)
                         .setContainerName(CONTAINER_NAME)
                         .setFirstSampleStart(HTimestamps.fromInstant(anyTime.plus(Duration.ofMinutes(51))))
                         .setLastSampleStart(HTimestamps.fromInstant(msgLastSampleStart))
                         .setTotalSamplesCount(msgSamplesCount)
                         .build())
            .build()));
    ArgumentCaptor<K8sWorkloadRecommendation> captor = ArgumentCaptor.forClass(K8sWorkloadRecommendation.class);
    verify(workloadRecommendationDao).save(captor.capture());
    assertThat(captor.getAllValues()).hasSize(1);

    K8sWorkloadRecommendation recommendation = captor.getValue();
    assertThat(recommendation.isDirty()).isTrue();
    ContainerCheckpoint containerCheckpoint = recommendation.getContainerCheckpoints().get("CONTAINER_NAME");
    assertThat(containerCheckpoint.getTotalSamplesCount()).isEqualTo(originalSamplesCount + msgSamplesCount);
    assertThat(containerCheckpoint.getLastSampleStart()).isEqualTo(msgLastSampleStart);
  }
}
