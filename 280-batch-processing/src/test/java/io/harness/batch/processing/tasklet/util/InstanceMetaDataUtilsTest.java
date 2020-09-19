package io.harness.batch.processing.tasklet.util;

import static io.harness.rule.OwnerRule.UTSAV;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.batch.processing.BatchProcessingBaseTest;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.batch.processing.writer.constants.K8sCCMConstants;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;

public class InstanceMetaDataUtilsTest extends BatchProcessingBaseTest {
  private static final String NODE_POOL_NAME = "manager-pool";

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testPopulateNodePoolNameFromLabel() throws Exception {
    assertNodePoolName(ImmutableMap.of(K8sCCMConstants.GKE_NODE_POOL_KEY, NODE_POOL_NAME));
    assertNodePoolName(ImmutableMap.of(K8sCCMConstants.AKS_NODE_POOL_KEY, NODE_POOL_NAME));
    assertNodePoolName(ImmutableMap.of(K8sCCMConstants.EKS_NODE_POOL_KEY, NODE_POOL_NAME));
  }

  private void assertNodePoolName(Map<String, String> labelsMap) {
    Map<String, String> metaData = new HashMap<>();

    InstanceMetaDataUtils.populateNodePoolNameFromLabel(labelsMap, metaData);
    assertThat(metaData.get(InstanceMetaDataConstants.NODE_POOL_NAME)).isEqualTo(NODE_POOL_NAME);
  }
}