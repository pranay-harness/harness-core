/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.dao.ClusterRecordDao;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.commons.entities.k8s.recommendation.PartialRecommendationHistogram;
import io.harness.histogram.HistogramCheckpoint;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLContainerHistogramData;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLHistogramExp;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8SWorkloadHistogramData;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8sWorkloadParameters;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerCheckpoint;
import software.wings.security.UserThreadLocal;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class K8sWorkloadHistogramDataFetcherTest extends AbstractDataFetcherTestBase {
  @Inject private HPersistence hPersistence;
  @Inject private K8sWorkloadHistogramDataFetcher k8sWorkloadHistogramDataFetcher;
  @Inject private ClusterRecordDao clusterRecordDao;

  private String clusterId;

  private Instant refDate;

  @Before
  public void setUp() throws Exception {
    Account account = testUtils.createAccount();
    User user = testUtils.createUser(account);
    UserThreadLocal.set(user);
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    clusterId = clusterRecordDao
                    .upsertCluster(ClusterRecord.builder()
                                       .accountId(ACCOUNT1_ID)
                                       .cluster(DirectKubernetesCluster.builder()
                                                    .clusterName(CLUSTER1_NAME)
                                                    .cloudProviderId(CLOUD_PROVIDER1_ID_ACCOUNT1)
                                                    .build())
                                       .isDeactivated(true)
                                       .build())
                    .getUuid();
    refDate = Instant.now().truncatedTo(ChronoUnit.DAYS);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldFetchHistogram() throws Exception {
    List<PartialRecommendationHistogram> histograms =
        Arrays.asList(PartialRecommendationHistogram.builder()
                          .accountId(ACCOUNT1_ID)
                          .clusterId(clusterId)
                          .namespace("default")
                          .workloadName("my-nginx")
                          .workloadType("Deployment")
                          .date(refDate.plus(Duration.ofDays(2)))
                          .containerCheckpoints(ImmutableMap.<String, ContainerCheckpoint>builder()
                                                    .put("nginx",
                                                        ContainerCheckpoint.builder()
                                                            .cpuHistogram(HistogramCheckpoint.builder()
                                                                              .totalWeight(1)
                                                                              .bucketWeights(ImmutableMap.of(0, 10000))
                                                                              .build())
                                                            .memoryPeak(1500000000L)
                                                            .build())
                                                    .build())
                          .build(),
            PartialRecommendationHistogram.builder()
                .accountId(ACCOUNT1_ID)
                .clusterId(clusterId)
                .namespace("default")
                .workloadName("my-nginx")
                .workloadType("Deployment")
                .date(refDate.plus(Duration.ofDays(3)))
                .containerCheckpoints(ImmutableMap.<String, ContainerCheckpoint>builder()
                                          .put("nginx",
                                              ContainerCheckpoint.builder()
                                                  .cpuHistogram(HistogramCheckpoint.builder()
                                                                    .totalWeight(1)
                                                                    .bucketWeights(ImmutableMap.of(2, 10000))
                                                                    .build())
                                                  .memoryPeak(2000000000L)
                                                  .build())
                                          .build())
                .build());
    hPersistence.save(histograms);
    QLK8SWorkloadHistogramData qlK8SWorkloadHistogramData =
        k8sWorkloadHistogramDataFetcher.fetch(QLK8sWorkloadParameters.builder()
                                                  .cluster(clusterId)
                                                  .namespace("default")
                                                  .workloadName("my-nginx")
                                                  .workloadType("Deployment")
                                                  .startDate(refDate.toEpochMilli())
                                                  .endDate(refDate.plus(Duration.ofDays(10)).toEpochMilli())
                                                  .build(),
            ACCOUNT1_ID);
    assertThat(qlK8SWorkloadHistogramData.getContainerHistogramDataList()).hasSize(1);
    QLContainerHistogramData qlContainerHistogramData =
        qlK8SWorkloadHistogramData.getContainerHistogramDataList().get(0);
    assertThat(qlContainerHistogramData.getContainerName()).isEqualTo("nginx");
    QLHistogramExp cpuHistogram = qlContainerHistogramData.getCpuHistogram();
    assertThat(cpuHistogram.getFirstBucketSize()).isEqualTo(0.01);
    assertThat(cpuHistogram.getMinBucket()).isEqualTo(0);
    assertThat(cpuHistogram.getMaxBucket()).isEqualTo(2);
    assertThat(cpuHistogram.getGrowthRatio()).isEqualTo(0.05);
    assertThat(cpuHistogram.getTotalWeight()).isEqualTo(2.0);
    assertThat(cpuHistogram.getNumBuckets()).isEqualTo(3);
    assertThat(cpuHistogram.getBucketWeights()[0]).isEqualTo(1.0);
    assertThat(cpuHistogram.getBucketWeights()[2]).isEqualTo(1.0);

    QLHistogramExp memoryHistogram = qlContainerHistogramData.getMemoryHistogram();
    assertThat(memoryHistogram.getFirstBucketSize()).isEqualTo(1E7);
    assertThat(memoryHistogram.getMinBucket()).isEqualTo(43);
    assertThat(memoryHistogram.getMaxBucket()).isEqualTo(49);
    assertThat(memoryHistogram.getGrowthRatio()).isEqualTo(0.05);
    assertThat(memoryHistogram.getTotalWeight()).isEqualTo(2.0);
    assertThat(memoryHistogram.getNumBuckets()).isEqualTo(7);
    assertThat(memoryHistogram.getBucketWeights()[0]).isEqualTo(1.0);
    assertThat(memoryHistogram.getBucketWeights()[6]).isEqualTo(1.0);
  }
}
