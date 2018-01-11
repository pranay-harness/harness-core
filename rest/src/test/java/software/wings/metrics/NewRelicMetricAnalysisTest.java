package software.wings.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;

import java.util.TreeSet;

/**
 * Created by rsingh on 9/7/17.
 */
public class NewRelicMetricAnalysisTest {
  @Test
  public void testCompare() {
    NewRelicMetricAnalysis analysis1 =
        NewRelicMetricAnalysis.builder().metricName("metric1").riskLevel(RiskLevel.HIGH).build();
    NewRelicMetricAnalysis analysis2 =
        NewRelicMetricAnalysis.builder().metricName("metric0").riskLevel(RiskLevel.MEDIUM).build();

    assertTrue(analysis1.compareTo(analysis2) < 0);
    TreeSet<NewRelicMetricAnalysis> treeSet = new TreeSet<>();
    treeSet.add(analysis2);
    treeSet.add(analysis1);

    assertEquals(analysis1, treeSet.first());

    analysis2.setRiskLevel(RiskLevel.HIGH);
    assertTrue(analysis1.compareTo(analysis2) > 0);

    treeSet.clear();
    treeSet.add(analysis1);
    treeSet.add(analysis2);
    assertEquals(analysis2, treeSet.first());

    analysis1.setMetricName("metric0");
    assertTrue(analysis1.compareTo(analysis2) == 0);

    NewRelicMetricAnalysis analysis3 =
        NewRelicMetricAnalysis.builder().metricName("abc").riskLevel(RiskLevel.HIGH).build();
    assertTrue(analysis3.compareTo(analysis1) < 0);
  }
}
