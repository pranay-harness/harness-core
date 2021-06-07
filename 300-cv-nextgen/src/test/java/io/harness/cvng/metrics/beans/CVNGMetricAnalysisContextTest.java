package io.harness.cvng.metrics.beans;

import static io.harness.cvng.metrics.CVNGMetricsUtils.METRIC_LABEL_PREFIX;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Map;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CVNGMetricAnalysisContextTest extends CvNextGenTestBase {
  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testAddLabels() throws Exception {
    Map<String, String> context = ThreadContext.getContext();
    CVNGMetricAnalysisContext analysisContext = new CVNGMetricAnalysisContext("myAccount", "myTask");

    // the 2 items should be added to context now.
    assertThat(context.size() + 2).isEqualTo(ThreadContext.getContext().size());
    assertThat(ThreadContext.get(METRIC_LABEL_PREFIX + "verificationTaskId")).isEqualTo("myTask");
    assertThat(ThreadContext.get(METRIC_LABEL_PREFIX + "accountId")).isEqualTo("myAccount");
    analysisContext.close();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testClose() throws Exception {
    Map<String, String> context = ThreadContext.getContext();
    CVNGMetricAnalysisContext analysisContext = new CVNGMetricAnalysisContext("myAccount", "myTask");

    // the 2 items should be added to context now.
    assertThat(context.size() + 2).isEqualTo(ThreadContext.getContext().size());
    assertThat(ThreadContext.get(METRIC_LABEL_PREFIX + "verificationTaskId")).isEqualTo("myTask");
    assertThat(ThreadContext.get(METRIC_LABEL_PREFIX + "accountId")).isEqualTo("myAccount");
    analysisContext.close();
    Map<String, String> closedContext = ThreadContext.getContext();

    assertThat(closedContext).isEqualTo(context);
  }
}
