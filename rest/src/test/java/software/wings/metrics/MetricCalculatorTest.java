package software.wings.metrics;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ArrayListMultimap;

import org.junit.Before;
import org.junit.Test;
import software.wings.metrics.BucketData.DataSummary;
import software.wings.metrics.MetricDefinition.ThresholdType;
import software.wings.metrics.appdynamics.AppdynamicsMetricDefinition;
import software.wings.service.impl.appdynamics.AppdynamicsMetricDataRecord;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by mike@ on 5/24/17.
 */
public class MetricCalculatorTest {
  private AppdynamicsMetricDefinition CALLS_METRIC_DEFINITION =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withAccountId("account_id")
          .withAppdynamicsAppId(5)
          .withMetricId("6")
          .withMetricName("Calls per Minute")
          .withMetricType(MetricType.COUNT)
          .withMediumThreshold(1)
          .withHighThreshold(2)
          .withThresholdType(ThresholdType.ALERT_WHEN_HIGHER)
          .build();
  private AppdynamicsMetricDefinition ART_METRIC_DEFINITION =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withAccountId("account_id")
          .withAppdynamicsAppId(5)
          .withMetricId("8")
          .withMetricName("Average Response Time (ms)")
          .withMetricType(MetricType.TIME)
          .withMediumThreshold(1)
          .withHighThreshold(2)
          .withThresholdType(ThresholdType.ALERT_WHEN_HIGHER)
          .build();
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_1 = createAppdynamicsMetricDataRecord(
      6, "Calls per Minute", MetricType.COUNT, 1, "tier1", 2, "todolist", "node1", 60000, 5);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_2 = createAppdynamicsMetricDataRecord(
      6, "Calls per Minute", MetricType.COUNT, 1, "tier1", 2, "todolist", "node2", 60000, 6);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_3 = createAppdynamicsMetricDataRecord(
      6, "Calls per Minute", MetricType.COUNT, 1, "tier1", 2, "todolist", "node3", 60000, 7);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_4 = createAppdynamicsMetricDataRecord(
      6, "Calls per Minute", MetricType.COUNT, 1, "tier1", 2, "todolist", "node4", 60000, 8);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_5 = createAppdynamicsMetricDataRecord(
      6, "Calls per Minute", MetricType.COUNT, 1, "tier1", 2, "todolist", "node1", 120000, 9);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_6 = createAppdynamicsMetricDataRecord(
      6, "Calls per Minute", MetricType.COUNT, 1, "tier1", 2, "todolist", "node2", 120000, 10);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_7 = createAppdynamicsMetricDataRecord(
      6, "Calls per Minute", MetricType.COUNT, 1, "tier1", 2, "todolist", "node3", 120000, 11);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_8 = createAppdynamicsMetricDataRecord(
      6, "Calls per Minute", MetricType.COUNT, 1, "tier1", 2, "todolist", "node4", 120000, 12);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_9 = createAppdynamicsMetricDataRecord(
      6, "Calls per Minute", MetricType.COUNT, 1, "tier1", 2, "todolist", "node1", 240000, 15);
  private AppdynamicsMetricDataRecord BT2_ART_RECORD_1 = createAppdynamicsMetricDataRecord(
      8, "Average Response Time (ms)", MetricType.TIME, 1, "tier1", 2, "todolist", "node1", 60000, 5);
  private AppdynamicsMetricDataRecord BT2_ART_RECORD_2 = createAppdynamicsMetricDataRecord(
      8, "Average Response Time (ms)", MetricType.TIME, 1, "tier1", 2, "todolist", "node2", 60000, 6);
  private AppdynamicsMetricDataRecord BT2_ART_RECORD_3 = createAppdynamicsMetricDataRecord(
      8, "Average Response Time (ms)", MetricType.TIME, 1, "tier1", 2, "todolist", "node3", 120000, 7);
  private AppdynamicsMetricDataRecord BT2_ART_RECORD_4 = createAppdynamicsMetricDataRecord(
      8, "Average Response Time (ms)", MetricType.TIME, 1, "tier1", 2, "todolist", "node4", 120000, 8);
  private AppdynamicsMetricDataRecord BT4_ART_RECORD_1 = createAppdynamicsMetricDataRecord(
      8, "Average Response Time (ms)", MetricType.TIME, 1, "tier1", 4, "login", "node1", 60000, 9);
  private AppdynamicsMetricDataRecord BT4_ART_RECORD_2 = createAppdynamicsMetricDataRecord(
      8, "Average Response Time (ms)", MetricType.TIME, 1, "tier1", 4, "login", "node2", 60000, 10);
  private AppdynamicsMetricDataRecord BT4_ART_RECORD_3 = createAppdynamicsMetricDataRecord(
      8, "Average Response Time (ms)", MetricType.TIME, 1, "tier1", 4, "login", "node1", 120000, 11);
  private AppdynamicsMetricDataRecord BT4_ART_RECORD_4 = createAppdynamicsMetricDataRecord(
      8, "Average Response Time (ms)", MetricType.TIME, 1, "tier1", 4, "login", "node2", 120000, 12);

  @Test
  public void shouldCalculateMetrics() {
    List<MetricDefinition> metricDefinitions = Arrays.asList(CALLS_METRIC_DEFINITION, ART_METRIC_DEFINITION);
    ArrayListMultimap<String, AppdynamicsMetricDataRecord> data = ArrayListMultimap.create();
    data.put("todolist", BT2_CALL_RECORD_1);
    data.put("todolist", BT2_CALL_RECORD_2);
    data.put("todolist", BT2_CALL_RECORD_3);
    data.put("todolist", BT2_CALL_RECORD_4);
    data.put("todolist", BT2_CALL_RECORD_5);
    data.put("todolist", BT2_CALL_RECORD_6);
    data.put("todolist", BT2_CALL_RECORD_7);
    data.put("todolist", BT2_CALL_RECORD_8);
    data.put("todolist", BT2_ART_RECORD_1);
    data.put("todolist", BT2_ART_RECORD_2);
    data.put("todolist", BT2_ART_RECORD_3);
    data.put("todolist", BT2_ART_RECORD_4);
    data.put("login", BT4_ART_RECORD_1);
    data.put("login", BT4_ART_RECORD_2);
    data.put("login", BT4_ART_RECORD_3);
    data.put("login", BT4_ART_RECORD_4);
    Map<String, Map<String, BucketData>> output = MetricCalculator.calculateMetrics(metricDefinitions, data);
    assertEquals(2, output.keySet().size());
    Map<String, BucketData> todolistMap = output.get("todolist");
    assertEquals(2, todolistMap.keySet().size());
    BucketData todolistCallData = todolistMap.get("Calls per Minute");
    assertEquals(60000, todolistCallData.getStartTimeMillis());
    assertEquals(180000, todolistCallData.getEndTimeMillis());
    assertEquals(RiskLevel.MEDIUM, todolistCallData.getRisk());
    assertEquals(4, todolistCallData.getOldData().getNodeCount());
    BucketData todolistArtData = todolistMap.get("Average Response Time (ms)");
    assertEquals(60000, todolistArtData.getStartTimeMillis());
    assertEquals(180000, todolistArtData.getEndTimeMillis());
    assertEquals(RiskLevel.MEDIUM, todolistArtData.getRisk());
    assertEquals(2, todolistArtData.getOldData().getNodeCount());
    BucketData loginArtData = output.get("login").get("Average Response Time (ms)");
    assertEquals(60000, loginArtData.getStartTimeMillis());
    assertEquals(180000, loginArtData.getEndTimeMillis());
    assertEquals(RiskLevel.MEDIUM, loginArtData.getRisk());
    assertEquals(2, loginArtData.getOldData().getNodeCount());

    // with missing metric definition
    metricDefinitions = Arrays.asList(CALLS_METRIC_DEFINITION);
    output = MetricCalculator.calculateMetrics(metricDefinitions, data);
    assertEquals(2, output.keySet().size());
    todolistMap = output.get("todolist");
    assertEquals(2, todolistMap.keySet().size());
    todolistCallData = todolistMap.get("Calls per Minute");
    assertEquals(60000, todolistCallData.getStartTimeMillis());
    assertEquals(180000, todolistCallData.getEndTimeMillis());
    assertEquals(RiskLevel.MEDIUM, todolistCallData.getRisk());
    assertEquals(4, todolistCallData.getOldData().getNodeCount());
    todolistArtData = todolistMap.get("Average Response Time (ms)");
    assertEquals(60000, todolistArtData.getStartTimeMillis());
    assertEquals(180000, todolistArtData.getEndTimeMillis());
    assertEquals(RiskLevel.MEDIUM, todolistArtData.getRisk());
    assertEquals(2, todolistArtData.getOldData().getNodeCount());
    loginArtData = output.get("login").get("Average Response Time (ms)");
    assertEquals(60000, loginArtData.getStartTimeMillis());
    assertEquals(180000, loginArtData.getEndTimeMillis());
    assertEquals(RiskLevel.MEDIUM, loginArtData.getRisk());
    assertEquals(2, loginArtData.getOldData().getNodeCount());
  }

  @Test
  public void shouldSplitDataIntoOldAndNew() {}

  @Test
  public void shouldParse() {
    List<AppdynamicsMetricDataRecord> oldRecords =
        Arrays.asList(BT2_CALL_RECORD_1, BT2_CALL_RECORD_2, BT2_CALL_RECORD_5, BT2_CALL_RECORD_6);
    List<AppdynamicsMetricDataRecord> newRecords =
        Arrays.asList(BT2_CALL_RECORD_3, BT2_CALL_RECORD_4, BT2_CALL_RECORD_7, BT2_CALL_RECORD_8);
    List<List<AppdynamicsMetricDataRecord>> records = Arrays.asList(oldRecords, newRecords);
    BucketData bucketData = MetricCalculator.parse(CALLS_METRIC_DEFINITION, records);
    assertEquals(60000, bucketData.getStartTimeMillis());
    assertEquals(180000, bucketData.getEndTimeMillis());
    // newRecords sum = 38, oldRecords sum = 30, so medium risk
    assertEquals(RiskLevel.MEDIUM, bucketData.getRisk());
    assertEquals(2, bucketData.getOldData().getNodeCount());
    assertEquals(2, bucketData.getNewData().getNodeCount());
    assertEquals("30.0", bucketData.getOldData().getDisplayValue());
    assertEquals("38.0", bucketData.getNewData().getDisplayValue());
  }

  @Test
  public void shouldParsePartial() {
    List<AppdynamicsMetricDataRecord> CALL_RECORDS =
        Arrays.asList(BT2_CALL_RECORD_1, BT2_CALL_RECORD_2, BT2_CALL_RECORD_3, BT2_CALL_RECORD_4, BT2_CALL_RECORD_5,
            BT2_CALL_RECORD_6, BT2_CALL_RECORD_7, BT2_CALL_RECORD_8);
    DataSummary summary = MetricCalculator.parsePartial(CALLS_METRIC_DEFINITION, CALL_RECORDS);
    assertEquals(4, summary.getNodeCount());
    assertEquals(68, summary.getStats().sum(), 0.05);
    assertEquals("68.0", summary.getDisplayValue());
    assertEquals(false, summary.isMissingData());

    // with some missing data
    List<AppdynamicsMetricDataRecord> CALL_RECORDS_WITH_MISSING_3 = Arrays.asList(BT2_CALL_RECORD_1, BT2_CALL_RECORD_2,
        BT2_CALL_RECORD_4, BT2_CALL_RECORD_5, BT2_CALL_RECORD_6, BT2_CALL_RECORD_7, BT2_CALL_RECORD_8);
    summary = MetricCalculator.parsePartial(CALLS_METRIC_DEFINITION, CALL_RECORDS_WITH_MISSING_3);
    assertEquals(4, summary.getNodeCount());
    assertEquals(61, summary.getStats().sum(), 0.05);
    assertEquals("61.0", summary.getDisplayValue());
    assertEquals(true, summary.isMissingData());

    // with a gap in the times of the records
    List<AppdynamicsMetricDataRecord> CALL_RECORDS_WITH_EXTRA_LATE_DATA =
        Arrays.asList(BT2_CALL_RECORD_1, BT2_CALL_RECORD_5, BT2_CALL_RECORD_9);
    summary = MetricCalculator.parsePartial(CALLS_METRIC_DEFINITION, CALL_RECORDS_WITH_EXTRA_LATE_DATA);
    assertEquals(1, summary.getNodeCount());
    assertEquals(29, summary.getStats().sum(), 0.05);
    assertEquals("29.0", summary.getDisplayValue());
    assertEquals(true, summary.isMissingData());

    // with time metric type
    CALLS_METRIC_DEFINITION.setMetricType(MetricType.TIME);
    summary = MetricCalculator.parsePartial(CALLS_METRIC_DEFINITION, CALL_RECORDS);
    assertEquals(4, summary.getNodeCount());
    assertEquals(8.5, summary.getStats().mean(), 0.05);
    assertEquals("8.5", summary.getDisplayValue());
    assertEquals(false, summary.isMissingData());
  }

  private AppdynamicsMetricDataRecord createAppdynamicsMetricDataRecord(long metricId, String metricName,
      MetricType metricType, long tierId, String tierName, long btId, String btName, String nodeName, long startTime,
      long value) {
    return AppdynamicsMetricDataRecord.Builder.anAppdynamicsMetricsDataRecord()
        .withAccountId("account_id")
        .withAppdAppId(5)
        .withMetricId(metricId)
        .withMetricName(metricName)
        .withMetricType(metricType)
        .withTierId(tierId)
        .withTierName(tierName)
        .withBtId(btId)
        .withBtName(btName)
        .withNodeName(nodeName)
        .withStartTime(startTime)
        .withValue(value)
        .withMin(value)
        .withMax(value)
        .withCurrent(value)
        .withSum(value)
        .withCount(1)
        .withStandardDeviation(0.0)
        .withOccurrences(0)
        .withUseRange(true)
        .build();
  }
}
