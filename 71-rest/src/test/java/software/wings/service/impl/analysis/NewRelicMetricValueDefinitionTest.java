package software.wings.service.impl.analysis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.metrics.MetricType;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricHostAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.sm.StateType;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NewRelicMetricValueDefinitionTest extends WingsBaseTest {
  private static final SecureRandom random = new SecureRandom();

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetTestHostValues() {
    String stateExecutionId = generateUuid();
    int numOfHosts = 5;
    int numOfRecordsPerHost = 3;
    Set<NewRelicMetricDataRecord> records = new HashSet<>();
    for (int i = 0; i < numOfHosts; i++) {
      for (int j = 0; j < numOfRecordsPerHost; j++) {
        Map<String, Double> metricValues = new HashMap<>();
        metricValues.put(NewRelicMetricValueDefinition.ERROR, random.nextDouble());
        metricValues.put(NewRelicMetricValueDefinition.AVERAGE_RESPONSE_TIME, random.nextDouble());
        metricValues.put(NewRelicMetricValueDefinition.APDEX_SCORE, random.nextDouble());
        metricValues.put(NewRelicMetricValueDefinition.AVERAGE_RESPONSE_TIME, random.nextDouble());
        records.add(NewRelicMetricDataRecord.builder()
                        .stateExecutionId(stateExecutionId)
                        .stateType(StateType.NEW_RELIC)
                        .host("host" + i)
                        .values(metricValues)
                        .timeStamp(j)
                        .build());
      }
    }
    NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                              .metricName(NewRelicMetricValueDefinition.ERROR)
                                                              .metricValueName(NewRelicMetricValueDefinition.ERROR)
                                                              .metricType(MetricType.ERROR)
                                                              .build();
    final List<NewRelicMetricHostAnalysisValue> testHostValues = metricValueDefinition.getTestHostValues(records);
    assertThat(testHostValues.size()).isEqualTo(numOfHosts);
    testHostValues.forEach(
        testHostValue -> assertThat(testHostValue.getTestValues().size()).isEqualTo(numOfRecordsPerHost));
  }
}
