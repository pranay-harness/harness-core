package software.wings.service.impl.analysis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.service.impl.analysis.AnalysisContext.AnalysisContextKeys;

import java.util.Random;

public class AnalysisContextTest extends WingsBaseTest {
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testIteration() throws IllegalAccessException {
    final AnalysisContext analysisContext = AnalysisContext.builder().stateExecutionId(generateUuid()).build();
    Random r = new Random();
    long timeSeriesAnalysisIteration = r.nextLong();
    FieldUtils.writeField(
        analysisContext, AnalysisContextKeys.timeSeriesAnalysisIteration, timeSeriesAnalysisIteration, true);
    long logAnalysisIteration = r.nextLong();
    FieldUtils.writeField(analysisContext, AnalysisContextKeys.logAnalysisIteration, logAnalysisIteration, true);

    assertThat(analysisContext.obtainNextIteration(AnalysisContextKeys.timeSeriesAnalysisIteration))
        .isEqualTo(timeSeriesAnalysisIteration);
    assertThat(analysisContext.obtainNextIteration(AnalysisContextKeys.logAnalysisIteration))
        .isEqualTo(logAnalysisIteration);

    timeSeriesAnalysisIteration = r.nextLong();
    analysisContext.updateNextIteration(AnalysisContextKeys.timeSeriesAnalysisIteration, timeSeriesAnalysisIteration);
    assertThat(analysisContext.obtainNextIteration(AnalysisContextKeys.timeSeriesAnalysisIteration))
        .isEqualTo(timeSeriesAnalysisIteration);

    logAnalysisIteration = r.nextLong();
    analysisContext.updateNextIteration(AnalysisContextKeys.logAnalysisIteration, logAnalysisIteration);
    assertThat(analysisContext.obtainNextIteration(AnalysisContextKeys.logAnalysisIteration))
        .isEqualTo(logAnalysisIteration);

    try {
      analysisContext.updateNextIteration(generateUuid(), r.nextLong());
      fail("Did not throw exception");
    } catch (IllegalArgumentException e) {
      // expected
    }

    try {
      analysisContext.obtainNextIteration(generateUuid());
      fail("Did not throw exception");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
}
