package io.harness.batch.processing.tasklet;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.recommendation.NodePoolId;
import io.harness.ccm.commons.beans.recommendation.TotalResourceUsage;
import io.harness.ccm.commons.dao.recommendation.K8sRecommendationDAO;
import io.harness.rule.Owner;
import io.harness.testsupport.BaseTaskletTest;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class K8sNodeRecommendationTaskletTest extends BaseTaskletTest {
  @Mock private K8sRecommendationDAO k8sRecommendationDAO;
  @InjectMocks private K8sNodeRecommendationTasklet tasklet;

  @Before
  public void setUp() throws Exception {
    mockChunkContext();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testExecute() throws Exception {
    String NODE_POOL_NAME = "nodePoolName";
    String CLUSTER_ID = "clusterId";
    NodePoolId nodePoolId = NodePoolId.builder().clusterid(CLUSTER_ID).nodepoolname(NODE_POOL_NAME).build();
    TotalResourceUsage resourceUsage =
        TotalResourceUsage.builder().maxcpu(100).maxmemory(100).sumcpu(200).summemory(200).build();

    when(k8sRecommendationDAO.getUniqueNodePools(eq(ACCOUNT_ID))).thenReturn(ImmutableList.of(nodePoolId));
    when(k8sRecommendationDAO.maxResourceOfAllTimeBucketsForANodePool(any(), any())).thenReturn(resourceUsage);
    doNothing().when(k8sRecommendationDAO).insertNodePoolAggregated(any(), any(), any());

    assertThat(tasklet.execute(null, chunkContext)).isNull();
  }
}