package io.harness.batch.processing.config.k8s.recommendation.estimators;

import static io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils.cpuAmountFromCores;
import static io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils.makeResourceMap;
import static io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils.memoryAmountFromBytes;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

public class ConstEstimatorTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testConstEstimator() throws Exception {
    Map<String, Long> estimation = makeResourceMap(cpuAmountFromCores(1.234), memoryAmountFromBytes(9876.5));
    ResourceEstimator estimator = ConstEstimator.of(estimation);
    assertThat(estimator.getResourceEstimation(null)).isEqualTo(estimation);
  }
}
