package software.wings.metrics;

import static software.wings.metrics.appdynamics.AppdynamicsConstants.*;
import static org.hamcrest.collection.IsArrayContainingInAnyOrder.arrayContainingInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ArrayListMultimap;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.exception.WingsException;
import software.wings.metrics.BucketData.DataSummary;
import software.wings.metrics.MetricDefinition.Threshold;
import software.wings.metrics.MetricDefinition.ThresholdType;
import software.wings.metrics.MetricSummary.BTMetrics;
import software.wings.metrics.appdynamics.AppdynamicsMetricDefinition;
import software.wings.service.impl.appdynamics.AppdynamicsMetricDataRecord;

import java.util.Arrays;
import java.util.List;

/**
 * Created by mike@ on 5/24/17.
 */
public class MetricCalculatorTest {
  private final String ACCOUNT_ID = "account_id";
  private final long APPD_APP_ID = 5;
  private AppdynamicsMetricDefinition CALLS_METRIC_DEFINITION =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withAccountId(ACCOUNT_ID)
          .withAppdynamicsAppId(APPD_APP_ID)
          .withMetricId("0")
          .withMetricName(CALLS_PER_MINUTE)
          .withMetricType(MetricType.RATE)
          .withThreshold(ThresholdComparisonType.RATIO, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 1, 2))
          .build();
  private AppdynamicsMetricDefinition SLOW_CALLS_METRIC_DEFINITION =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withAccountId(ACCOUNT_ID)
          .withAppdynamicsAppId(APPD_APP_ID)
          .withMetricId("1")
          .withMetricName(NUMBER_OF_SLOW_CALLS)
          .withMetricType(MetricType.COUNT)
          .withThreshold(ThresholdComparisonType.RATIO, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 1, 2))
          .build();
  private AppdynamicsMetricDefinition VERY_SLOW_CALLS_METRIC_DEFINITION =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withAccountId(ACCOUNT_ID)
          .withAppdynamicsAppId(APPD_APP_ID)
          .withMetricId("2")
          .withMetricName(NUMBER_OF_VERY_SLOW_CALLS)
          .withMetricType(MetricType.COUNT)
          .withThreshold(ThresholdComparisonType.RATIO, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 1, 2))
          .build();
  private AppdynamicsMetricDefinition ERRORS_METRIC_DEFINITION =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withAccountId(ACCOUNT_ID)
          .withAppdynamicsAppId(APPD_APP_ID)
          .withMetricId("3")
          .withMetricName(ERRORS_PER_MINUTE)
          .withMetricType(MetricType.COUNT)
          .withThreshold(ThresholdComparisonType.RATIO, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 1, 2))
          .build();
  private AppdynamicsMetricDefinition STALLS_METRIC_DEFINITION =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withAccountId(ACCOUNT_ID)
          .withAppdynamicsAppId(APPD_APP_ID)
          .withMetricId("4")
          .withMetricName(STALL_COUNT)
          .withMetricType(MetricType.COUNT)
          .withThreshold(ThresholdComparisonType.RATIO, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 1, 2))
          .build();
  private AppdynamicsMetricDefinition ART_METRIC_DEFINITION =
      AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
          .withAccountId(ACCOUNT_ID)
          .withAppdynamicsAppId(APPD_APP_ID)
          .withMetricId("5")
          .withMetricName(RESPONSE_TIME_95)
          .withMetricType(MetricType.TIME_MS)
          .withThreshold(ThresholdComparisonType.RATIO, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 1, 2))
          .build();
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_1 = createAppdynamicsMetricDataRecord(
      6, CALLS_PER_MINUTE, MetricType.RATE, 1, "tier1", 2, "todolist", "node1", 60000, 5);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_2 = createAppdynamicsMetricDataRecord(
      6, CALLS_PER_MINUTE, MetricType.RATE, 1, "tier1", 2, "todolist", "node2", 60000, 6);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_3 = createAppdynamicsMetricDataRecord(
      6, CALLS_PER_MINUTE, MetricType.RATE, 1, "tier1", 2, "todolist", "node3", 60000, 57);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_4 = createAppdynamicsMetricDataRecord(
      6, CALLS_PER_MINUTE, MetricType.RATE, 1, "tier1", 2, "todolist", "node4", 60000, 58);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_5 = createAppdynamicsMetricDataRecord(
      6, CALLS_PER_MINUTE, MetricType.RATE, 1, "tier1", 2, "todolist", "node1", 120000, 9);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_6 = createAppdynamicsMetricDataRecord(
      6, CALLS_PER_MINUTE, MetricType.RATE, 1, "tier1", 2, "todolist", "node2", 120000, 10);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_7 = createAppdynamicsMetricDataRecord(
      6, CALLS_PER_MINUTE, MetricType.RATE, 1, "tier1", 2, "todolist", "node3", 120000, 61);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_8 = createAppdynamicsMetricDataRecord(
      6, CALLS_PER_MINUTE, MetricType.RATE, 1, "tier1", 2, "todolist", "node4", 120000, 62);
  private AppdynamicsMetricDataRecord BT2_CALL_RECORD_9 = createAppdynamicsMetricDataRecord(
      6, CALLS_PER_MINUTE, MetricType.RATE, 1, "tier1", 2, "todolist", "node1", 240000, 15);
  private AppdynamicsMetricDataRecord BT2_SLOW_CALL_RECORD_1 = createAppdynamicsMetricDataRecord(
      6, NUMBER_OF_SLOW_CALLS, MetricType.RATE, 1, "tier1", 2, "todolist", "node1", 240000, 15);
  private AppdynamicsMetricDataRecord BT2_VERY_SLOW_CALL_RECORD_1 = createAppdynamicsMetricDataRecord(
      6, NUMBER_OF_VERY_SLOW_CALLS, MetricType.RATE, 1, "tier1", 2, "todolist", "node1", 240000, 10);
  private AppdynamicsMetricDataRecord BT2_VERY_SLOW_CALL_RECORD_2 = createAppdynamicsMetricDataRecord(
      6, NUMBER_OF_VERY_SLOW_CALLS, MetricType.RATE, 1, "tier1", 2, "todolist", "node2", 240000, 10);
  private AppdynamicsMetricDataRecord BT2_ART_RECORD_1 = createAppdynamicsMetricDataRecord(
      8, RESPONSE_TIME_95, MetricType.TIME_MS, 1, "tier1", 2, "todolist", "node1", 60000, 5);
  private AppdynamicsMetricDataRecord BT2_ART_RECORD_2 = createAppdynamicsMetricDataRecord(
      8, RESPONSE_TIME_95, MetricType.TIME_MS, 1, "tier1", 2, "todolist", "node2", 60000, 6);
  private AppdynamicsMetricDataRecord BT2_ART_RECORD_3 = createAppdynamicsMetricDataRecord(
      8, RESPONSE_TIME_95, MetricType.TIME_MS, 1, "tier1", 2, "todolist", "node3", 120000, 57);
  private AppdynamicsMetricDataRecord BT2_ART_RECORD_4 = createAppdynamicsMetricDataRecord(
      8, RESPONSE_TIME_95, MetricType.TIME_MS, 1, "tier1", 2, "todolist", "node4", 120000, 58);
  private AppdynamicsMetricDataRecord BT4_ART_RECORD_1 = createAppdynamicsMetricDataRecord(
      8, RESPONSE_TIME_95, MetricType.TIME_MS, 1, "tier1", 4, "login", "node1", 60000, 9);
  private AppdynamicsMetricDataRecord BT4_ART_RECORD_2 = createAppdynamicsMetricDataRecord(
      8, RESPONSE_TIME_95, MetricType.TIME_MS, 1, "tier1", 4, "login", "node2", 60000, 10);
  private AppdynamicsMetricDataRecord BT4_ART_RECORD_3 = createAppdynamicsMetricDataRecord(
      8, RESPONSE_TIME_95, MetricType.TIME_MS, 1, "tier1", 4, "login", "node1", 120000, 11);
  private AppdynamicsMetricDataRecord BT4_ART_RECORD_4 = createAppdynamicsMetricDataRecord(
      8, RESPONSE_TIME_95, MetricType.TIME_MS, 1, "tier1", 4, "login", "node2", 120000, 12);
  private AppdynamicsMetricDataRecord BT2_UNKNOWN_RECORD_4 =
      createAppdynamicsMetricDataRecord(8, "foo", MetricType.TIME_MS, 1, "tier1", 2, "todolist", "node2", 120000, 12);
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
    MetricSummary output = MetricCalculator.calculateMetrics(metricDefinitions, data, Arrays.asList("node3", "node4"));
    assertEquals(ACCOUNT_ID, output.getAccountId());
    assertEquals(60000, output.getStartTimeMillis());
    assertEquals(180000, output.getEndTimeMillis());
    assertEquals(2, output.getBtMetricsMap().size());
    BTMetrics todolistMetrics = output.getBtMetricsMap().get("todolist");
    assertEquals(3, todolistMetrics.getMetricsMap().size());
    assertEquals(RiskLevel.HIGH, todolistMetrics.getBtRisk());
    assertEquals(1, todolistMetrics.getBtRiskSummary().size());
    BucketData todolistCallData = todolistMetrics.getMetricsMap().get(CALLS_PER_MINUTE);
    System.out.println(todolistCallData);
    assertEquals(RiskLevel.LOW, todolistCallData.getRisk());
    assertEquals(2, todolistCallData.getOldData().getNodeCount());
    BucketData todolistArtData = todolistMetrics.getMetricsMap().get(RESPONSE_TIME_95);
    assertEquals(RiskLevel.HIGH, todolistArtData.getRisk());
    assertEquals(2, todolistArtData.getOldData().getNodeCount());
    BucketData loginArtData = output.getBtMetricsMap().get("login").getMetricsMap().get(RESPONSE_TIME_95);
    assertEquals(RiskLevel.LOW, loginArtData.getRisk());
    assertEquals(2, loginArtData.getOldData().getNodeCount());
    assertEquals(1, output.getRiskMessages().size());
    assertEquals("todolist", output.getRiskMessages().get(0));

    ObjectMapper mapper = new ObjectMapper();
    try {
      String b = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);
      System.out.println(b);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    // with missing metric definition
    metricDefinitions = Arrays.asList(CALLS_METRIC_DEFINITION);
    output = MetricCalculator.calculateMetrics(metricDefinitions, data, Arrays.asList("node3", "node4"));
    assertEquals(2, output.getBtMetricsMap().size());
    todolistMetrics = output.getBtMetricsMap().get("todolist");
    assertEquals(3, todolistMetrics.getMetricsMap().size());
    assertEquals(RiskLevel.HIGH, todolistMetrics.getBtRisk());
    assertEquals(1, todolistMetrics.getBtRiskSummary().size());
    todolistCallData = todolistMetrics.getMetricsMap().get(CALLS_PER_MINUTE);
    assertEquals(RiskLevel.LOW, todolistCallData.getRisk());
    assertEquals(2, todolistCallData.getOldData().getNodeCount());
    todolistArtData = todolistMetrics.getMetricsMap().get(RESPONSE_TIME_95);
    assertEquals(RiskLevel.HIGH, todolistArtData.getRisk());
    assertEquals(2, todolistArtData.getOldData().getNodeCount());
    loginArtData = output.getBtMetricsMap().get("login").getMetricsMap().get(RESPONSE_TIME_95);
    assertEquals(RiskLevel.LOW, loginArtData.getRisk());
    assertEquals(2, loginArtData.getOldData().getNodeCount());
    assertEquals(1, output.getRiskMessages().size());
    assertEquals("todolist", output.getRiskMessages().get(0));

    // merge slow and very slow calls
    metricDefinitions = Arrays.asList(SLOW_CALLS_METRIC_DEFINITION, VERY_SLOW_CALLS_METRIC_DEFINITION);
    ArrayListMultimap<String, AppdynamicsMetricDataRecord> dataMerge = ArrayListMultimap.create();
    dataMerge.put("todolist", BT2_SLOW_CALL_RECORD_1);
    dataMerge.put("todolist", BT2_VERY_SLOW_CALL_RECORD_1);
    dataMerge.put("todolist", BT2_VERY_SLOW_CALL_RECORD_2);
    output = MetricCalculator.calculateMetrics(metricDefinitions, dataMerge, Arrays.asList("node2"));
    todolistMetrics = output.getBtMetricsMap().get("todolist");
    BucketData slowData = todolistMetrics.getMetricsMap().get(NUMBER_OF_SLOW_CALLS);
    assertEquals(25, slowData.getOldData().getValue(), 0.05);
    assertEquals(10, slowData.getNewData().getValue(), 0.05);
    BucketData verySlowData = todolistMetrics.getMetricsMap().get(NUMBER_OF_VERY_SLOW_CALLS);
    assertEquals(10, verySlowData.getNewData().getValue(), 0.05);
    assertEquals(10, verySlowData.getOldData().getValue(), 0.05);
    /*
        // with unknown metric type
        data.put("todolist", BT2_UNKNOWN_RECORD_4);
        try {
          output = MetricCalculator.calculateMetrics(metricDefinitions, data, Arrays.asList("node3", "node4"));
          fail("Expected a WingsException to be thrown");
        } catch (WingsException we) {
          assertEquals("Unexpected metric type: foo", we.getMessage());
        }
        */
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
    // newRecords sum = 38, oldRecords sum = 30, so medium risk
    assertEquals(RiskLevel.HIGH, bucketData.getRisk());
    assertEquals(MetricType.RATE, bucketData.getMetricType());
    assertEquals(2, bucketData.getOldData().getNodeCount());
    assertEquals(2, bucketData.getNewData().getNodeCount());
    assertEquals(7.5d, bucketData.getOldData().getValue(), 0.05);
    assertEquals(59.5d, bucketData.getNewData().getValue(), 0.05);
  }

  @Test
  public void shouldHandleMultipleThresholds() {
    AppdynamicsMetricDataRecord RECORD_1 = createAppdynamicsMetricDataRecord(
        8, RESPONSE_TIME_95, MetricType.TIME_MS, 1, "tier1", 4, "login", "node1", 60000, 9);
    AppdynamicsMetricDataRecord RECORD_2 = createAppdynamicsMetricDataRecord(
        8, RESPONSE_TIME_95, MetricType.TIME_MS, 1, "tier1", 4, "login", "node2", 60000, 15);
    // ratio is 1.6667, delta is 6
    AppdynamicsMetricDefinition RESP_METRIC_DEFINITION =
        AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
            .withAccountId(ACCOUNT_ID)
            .withAppdynamicsAppId(APPD_APP_ID)
            .withMetricId("5")
            .withMetricName(RESPONSE_TIME_95)
            .withMetricType(MetricType.TIME_MS)
            .withThreshold(ThresholdComparisonType.RATIO, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 1, 1.3))
            .withThreshold(ThresholdComparisonType.DELTA, new Threshold(ThresholdType.ALERT_WHEN_HIGHER, 0.01, 20))
            .build();
    List<List<AppdynamicsMetricDataRecord>> records = Arrays.asList(Arrays.asList(RECORD_1), Arrays.asList(RECORD_2));
    BucketData bucketData = MetricCalculator.parse(RESP_METRIC_DEFINITION, records);
    assertEquals(RiskLevel.MEDIUM, bucketData.getRisk());
    assertEquals(MetricType.TIME_MS, bucketData.getMetricType());
    assertEquals(1, bucketData.getOldData().getNodeCount());
    assertEquals(1, bucketData.getNewData().getNodeCount());
    assertEquals(9d, bucketData.getOldData().getValue(), 0.05);
    assertEquals(15d, bucketData.getNewData().getValue(), 0.05);
  }

  @Test
  public void shouldParsePartial() {
    List<AppdynamicsMetricDataRecord> CALL_RECORDS =
        Arrays.asList(BT2_CALL_RECORD_1, BT2_CALL_RECORD_2, BT2_CALL_RECORD_3, BT2_CALL_RECORD_4, BT2_CALL_RECORD_5,
            BT2_CALL_RECORD_6, BT2_CALL_RECORD_7, BT2_CALL_RECORD_8);
    DataSummary summary = MetricCalculator.parsePartial(CALLS_METRIC_DEFINITION, CALL_RECORDS);
    assertEquals(4, summary.getNodeCount());
    assertThat(summary.getNodeList().toArray(), arrayContainingInAnyOrder("node1", "node2", "node3", "node4"));
    assertEquals(33.5d, summary.getStats().mean(), 0.05);
    assertEquals(33.5d, summary.getValue(), 0.05);
    assertEquals(false, summary.isMissingData());

    // with some missing data
    List<AppdynamicsMetricDataRecord> CALL_RECORDS_WITH_MISSING_3 = Arrays.asList(BT2_CALL_RECORD_1, BT2_CALL_RECORD_2,
        BT2_CALL_RECORD_4, BT2_CALL_RECORD_5, BT2_CALL_RECORD_6, BT2_CALL_RECORD_7, BT2_CALL_RECORD_8);
    summary = MetricCalculator.parsePartial(CALLS_METRIC_DEFINITION, CALL_RECORDS_WITH_MISSING_3);
    assertEquals(4, summary.getNodeCount());
    assertEquals(29.25, summary.getStats().mean(), 0.05);
    assertEquals(29.25, summary.getValue(), 0.05);
    assertEquals(true, summary.isMissingData());

    // with a gap in the times of the records
    List<AppdynamicsMetricDataRecord> CALL_RECORDS_WITH_EXTRA_LATE_DATA =
        Arrays.asList(BT2_CALL_RECORD_1, BT2_CALL_RECORD_5, BT2_CALL_RECORD_9);
    summary = MetricCalculator.parsePartial(CALLS_METRIC_DEFINITION, CALL_RECORDS_WITH_EXTRA_LATE_DATA);
    assertEquals(1, summary.getNodeCount());
    assertEquals("node1", summary.getNodeList().get(0));
    assertEquals(29, summary.getStats().sum(), 0.05);
    assertEquals(9.667, summary.getStats().mean(), 0.05);
    assertEquals(9.667, summary.getValue(), 0.05);
    assertEquals(true, summary.isMissingData());

    // with time metric type
    CALLS_METRIC_DEFINITION.setMetricType(MetricType.TIME_MS);
    summary = MetricCalculator.parsePartial(CALLS_METRIC_DEFINITION, CALL_RECORDS);
    assertEquals(4, summary.getNodeCount());
    assertEquals(33.5, summary.getStats().mean(), 0.05);
    assertEquals(33.5, summary.getValue(), 0.05);
    assertEquals(false, summary.isMissingData());
  }

  @Test
  @Ignore
  public void generateFullSampleJson() {
    List<MetricDefinition> metricDefinitions = Arrays.asList(CALLS_METRIC_DEFINITION, SLOW_CALLS_METRIC_DEFINITION,
        VERY_SLOW_CALLS_METRIC_DEFINITION, ERRORS_METRIC_DEFINITION, STALLS_METRIC_DEFINITION, ART_METRIC_DEFINITION);
    ArrayListMultimap<String, AppdynamicsMetricDataRecord> data = ArrayListMultimap.create();
    String[] bts = new String[] {"todolist", "login"};
    String[] nodes = new String[] {"alpha", "beta", "gamma", "delta"};

    for (int b = 0; b < bts.length; b++) {
      for (MetricDefinition metric : metricDefinitions) {
        for (int n = 0; n < nodes.length; n++) {
          for (int s = 0; s < 3; s++) {
            int val = (int) (Math.random() * 100);
            data.put(bts[b],
                createAppdynamicsMetricDataRecord(Long.parseLong(metric.getMetricId()), metric.getMetricName(),
                    metric.getMetricType(), 1, "test-tier", b, bts[b], nodes[n], s * 60000, val));
          }
        }
      }
    }
    MetricSummary output = MetricCalculator.calculateMetrics(metricDefinitions, data, Arrays.asList("alpha", "beta"));
    ObjectMapper mapper = new ObjectMapper();
    try {
      String b = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);
      System.out.println(b);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
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
