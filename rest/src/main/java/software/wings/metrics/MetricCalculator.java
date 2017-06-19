package software.wings.metrics;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.math.Stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;
import software.wings.metrics.BucketData.DataSummary;
import software.wings.metrics.MetricDefinition.Threshold;
import software.wings.metrics.MetricDefinition.ThresholdType;
import software.wings.metrics.appdynamics.AppdynamicsConstants;
import software.wings.metrics.appdynamics.AppdynamicsMetricDefinition;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricDataRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by mike@ on 5/23/17.
 */
public class MetricCalculator {
  private static final Logger logger = LoggerFactory.getLogger(MetricCalculator.class);
  /**
   * Generate the per-BT/per-metric data.
   * @param metricDefinitions A list of MetricDefinitions that are represented in the metric data.
   * @param data A Multimap of metric names to all of the data records for that name.
   * @return A Map of BT names to a map of metric names to metric data.
   */
  public static MetricSummary calculateMetrics(List<MetricDefinition> metricDefinitions,
      ArrayListMultimap<String, AppdynamicsMetricDataRecord> data, List<String> newNodeNames) {
    if (data == null || data.keys() == null || data.values() == null || data.size() == 0) {
      return null;
    }
    Map<String, MetricSummary.BTMetrics> btMetricDataMap = new HashMap<>();
    // create a map of metric ID to metric definition
    Map<String, MetricDefinition> metricDefinitionMap = new HashMap<>();
    metricDefinitions.forEach(definition -> metricDefinitionMap.put(definition.getMetricId(), definition));

    String accountId = data.values().iterator().next().getAccountId();
    String appdAppId = data.values().iterator().next().getAppId();
    long startTimeMillis = System.currentTimeMillis();
    long endTimeMillis = 0;

    // split the data by btName
    for (String btName : data.keySet()) {
      // subsplit the per-bt data by metric
      ArrayListMultimap<MetricDefinition, AppdynamicsMetricDataRecord> metricData = ArrayListMultimap.create();
      for (AppdynamicsMetricDataRecord record : data.get(btName)) {
        startTimeMillis = Math.min(record.getStartTime(), startTimeMillis);
        endTimeMillis = Math.max(record.getStartTime(), endTimeMillis);
        // TODO: This is temporary logic until we build the interface to let people define metrics in the UI and persist
        // them If a metric doesn't have the corresponding metric definition, instead of throwing an exception, generate
        // an appropriate definition
        MetricDefinition metricDefinition;
        if (metricDefinitionMap.containsKey(String.valueOf(record.getMetricId()))) {
          metricDefinition = metricDefinitionMap.get(String.valueOf(record.getMetricId()));
        } else if (AppdynamicsConstants.METRIC_TEMPLATE_MAP.containsKey(record.getMetricName())) {
          metricDefinition = AppdynamicsConstants.METRIC_TEMPLATE_MAP.get(record.getMetricName())
                                 .withAccountId(record.getAccountId())
                                 .withAppdynamicsAppId(record.getAppdAppId())
                                 .withMetricId(String.valueOf(record.getMetricId()))
                                 .build();
        } else {
          metricDefinition = null;
          logger.debug("Unexpected metric type: " + record.getMetricName());
          // throw new WingsException(ErrorCode.APPDYNAMICS_ERROR, "Unexpected metric type: " + record.getMetricName(),
          // null);
        }
        if (metricDefinition != null) {
          metricData.put(metricDefinition, record);
        }
      }

      Map<String, BucketData> metricDataMap = new HashMap<>();
      for (MetricDefinition metricDefinition : metricData.keySet()) {
        List<AppdynamicsMetricDataRecord> singleMetricData = metricData.get(metricDefinition);
        // subsplit the per-bt/metric data by old/new
        List<List<AppdynamicsMetricDataRecord>> splitData = splitDataIntoOldAndNew(singleMetricData, newNodeNames);
        BucketData bucketData = parse(metricDefinition, splitData);
        metricDataMap.put(metricDefinition.getMetricName(), bucketData);
      }

      // create total calls metric if doesn't exist
      if (!metricDataMap.containsKey(AppdynamicsConstants.TOTAL_CALLS)
          && metricDataMap.containsKey(AppdynamicsConstants.CALLS_PER_MINUTE)) {
        BucketData bucketData = metricDataMap.get(AppdynamicsConstants.CALLS_PER_MINUTE);
        BucketData callsBucket = new BucketData();
        callsBucket.setRisk(bucketData.getRisk());
        callsBucket.setMetricType(MetricType.COUNT);
        if (bucketData.getOldData() != null) {
          DataSummary oldDataSummary = bucketData.getOldData();
          callsBucket.setOldData(new BucketData.DataSummary(oldDataSummary.getNodeCount(), oldDataSummary.getNodeList(),
              oldDataSummary.getStats(), oldDataSummary.getValue(), oldDataSummary.isMissingData()));
          callsBucket.getOldData().setValue(callsBucket.getOldData().getStats().sum()
              * (TimeUnit.MILLISECONDS.toMinutes(endTimeMillis - startTimeMillis) + 1));
        }
        if (bucketData.getNewData() != null) {
          DataSummary newDataSummary = bucketData.getNewData();
          callsBucket.setNewData(new BucketData.DataSummary(newDataSummary.getNodeCount(), newDataSummary.getNodeList(),
              newDataSummary.getStats(), newDataSummary.getValue(), newDataSummary.isMissingData()));
          callsBucket.getNewData().setValue(callsBucket.getNewData().getStats().sum()
              * (TimeUnit.MILLISECONDS.toMinutes(endTimeMillis - startTimeMillis) + 1));
        }
        metricDataMap.put(AppdynamicsConstants.TOTAL_CALLS, callsBucket);
      }

      // add very slow calls to slow calls
      if (metricDataMap.containsKey(AppdynamicsConstants.NUMBER_OF_VERY_SLOW_CALLS)
          && metricDataMap.containsKey(AppdynamicsConstants.NUMBER_OF_SLOW_CALLS)) {
        BucketData verySlowCalls = metricDataMap.get(AppdynamicsConstants.NUMBER_OF_VERY_SLOW_CALLS);
        BucketData slowCalls = metricDataMap.get(AppdynamicsConstants.NUMBER_OF_SLOW_CALLS);
        if (verySlowCalls.getOldData() != null) {
          if (slowCalls.getOldData() != null) {
            slowCalls.getOldData().setValue(slowCalls.getOldData().getValue() + verySlowCalls.getOldData().getValue());
          } else {
            slowCalls.setOldData(verySlowCalls.getOldData());
          }
        }
        if (verySlowCalls.getNewData() != null) {
          if (slowCalls.getNewData() != null) {
            slowCalls.getNewData().setValue(slowCalls.getNewData().getValue() + verySlowCalls.getNewData().getValue());
          } else {
            slowCalls.setNewData(verySlowCalls.getNewData());
          }
        }
        metricDataMap.put(AppdynamicsConstants.NUMBER_OF_SLOW_CALLS, slowCalls);
      }
      MetricSummary.BTMetrics btMetrics = calculateOverallBTRisk(metricDataMap);
      btMetricDataMap.put(btName, btMetrics);
    }
    endTimeMillis += TimeUnit.MINUTES.toMillis(1);

    RiskLevel risk = RiskLevel.LOW;
    for (String bt : btMetricDataMap.keySet()) {
      MetricSummary.BTMetrics btMetrics = btMetricDataMap.get(bt);
      if (btMetrics.getBtRisk().compareTo(risk) < 0) {
        risk = btMetrics.getBtRisk();
      }
    }

    List<String> riskMessages = new ArrayList<>();
    for (String bt : btMetricDataMap.keySet()) {
      MetricSummary.BTMetrics btMetrics = btMetricDataMap.get(bt);
      if (btMetrics.getBtRisk().equals(risk)) {
        riskMessages.add(bt);
      }
    }

    Collections.sort(riskMessages);
    MetricSummary metricSummary = MetricSummary.Builder.aMetricSummary()
                                      .withAccountId(accountId)
                                      .withBtMetricsMap(btMetricDataMap)
                                      .withRiskLevel(risk)
                                      .withRiskMessages(riskMessages)
                                      .withStartTimeMillis(startTimeMillis)
                                      .withEndTimeMillis(endTimeMillis)
                                      .build();
    return metricSummary;
  }

  /**
   * Divides a list of data records into those generated by the old build vs those generated by the new build.
   * @param data A list of data records for a single BT/metric.
   * @param newNodeNames A list of names of nodes that were pushed in the new build.
   * @return A list of two lists; the first is a list of records for the old build, the second is the list for the new.
   */
  public static List<List<AppdynamicsMetricDataRecord>> splitDataIntoOldAndNew(
      List<AppdynamicsMetricDataRecord> data, List<String> newNodeNames) {
    List<List<AppdynamicsMetricDataRecord>> output = new ArrayList<>();
    List<AppdynamicsMetricDataRecord> oldData = new ArrayList<>();
    List<AppdynamicsMetricDataRecord> newData = new ArrayList<>();
    for (AppdynamicsMetricDataRecord record : data) {
      if (newNodeNames.contains(record.getNodeName())) {
        newData.add(record);
      } else {
        oldData.add(record);
      }
    }
    output.add(oldData);
    output.add(newData);
    return output;
  }

  /**
   * Generate stats, display value, and risk level for a set of data records.
   * The output will be unreliable for Count metrics if there was partial data;
   * for example, if we had data for nodes 1, 2, 3 in minute 1 and nodes 1, 3
   * in minute 2, nodeCount will be 3 and the count per minute will look low
   * since it's dividing two nodes' worth of count by three.
   * @param metricDefinition The MetricDefinition for the metric in these records.
   * @param records The records.
   * @return A BucketData that contains the summary for the old and new datasets.
   */
  public static BucketData parse(MetricDefinition metricDefinition, List<List<AppdynamicsMetricDataRecord>> records) {
    List<AppdynamicsMetricDataRecord> oldRecords = records.get(0);
    List<AppdynamicsMetricDataRecord> newRecords = records.get(1);
    DataSummary oldSummary = parsePartial(metricDefinition, oldRecords);
    DataSummary newSummary = parsePartial(metricDefinition, newRecords);
    long startTimeMillis = System.currentTimeMillis();
    long endTimeMillis = 0;
    if (oldRecords != null && oldRecords.size() > 0) {
      startTimeMillis = oldRecords.get(0).getStartTime();
      endTimeMillis = oldRecords.get(oldRecords.size() - 1).getStartTime();
    }
    if (newRecords != null && newRecords.size() > 0) {
      startTimeMillis = Math.min(startTimeMillis, newRecords.get(0).getStartTime());
      endTimeMillis = Math.max(endTimeMillis, newRecords.get(newRecords.size() - 1).getStartTime());
    }
    endTimeMillis += TimeUnit.MINUTES.toMillis(1);

    /*
     * This risk level stuff is confusing. A quick overview:
     * The default is LOW.
     * Depending on the metric type, we generate the ratio/delta/absolute.
     * Now we'll set the risk level to the lowest level of all of the thresholds defined for the metric,
     *   excluding metrics that have ThresholdType.NO_ALERT. This is so that if a metric has multiple
     *   defined thresholds, such as:
     *     ratio above 1.3, delta greater than 20 = HIGH
     *     ratio above 1.0, delta greater than 0.01 = MEDIUM
     *   if ratio is above 1.3 but delta is between 0.01 and 20, we want to return MEDIUM.
     * relevantThreshold indicates whether we've seen any metrics that are not of type NO_ALERT, because
     *   if we just saw a single NO_ALERT and didn't have that logic, it'd end up setting the risk to HIGH.
     */
    RiskLevel risk = RiskLevel.LOW;
    // default thresholds are in AppdynamicsConstants
    double ratio = 0.0;
    double delta = 0.0;
    double absolute = 0.0;
    if (newSummary != null && oldSummary != null) {
      MetricType metricType = metricDefinition.getMetricType();
      if (metricType == MetricType.COUNT) {
        ratio = (newSummary.getStats().sum() / newSummary.getNodeCount())
            / (oldSummary.getStats().sum() / oldSummary.getNodeCount());
        delta = (newSummary.getStats().sum() / newSummary.getNodeCount())
            - (oldSummary.getStats().sum() / oldSummary.getNodeCount());
        absolute = newSummary.getStats().sum() / newSummary.getNodeCount();
      } else if (metricType == MetricType.PERCENTAGE || metricType == MetricType.TIME_MS
          || metricType == MetricType.TIME || metricType == MetricType.RATE) {
        ratio = newSummary.getStats().mean() / oldSummary.getStats().mean();
        delta = newSummary.getStats().mean() - oldSummary.getStats().mean();
        absolute = newSummary.getStats().mean();
      }
      double value = Double.MIN_VALUE;
      RiskLevel newRisk = RiskLevel.HIGH;
      boolean relevantThreshold = false;
      for (ThresholdComparisonType tct : metricDefinition.getThresholds().keySet()) {
        Threshold threshold = metricDefinition.getThresholds().get(tct);
        if (tct == ThresholdComparisonType.RATIO) {
          value = ratio;
        } else if (tct == ThresholdComparisonType.DELTA) {
          value = delta;
        } else if (tct == ThresholdComparisonType.ABSOLUTE) {
          value = absolute;
        } else {
          logger.warn("Metric with undefined comparison type: " + metricDefinition);
        }
        if (value != Double.MIN_VALUE && threshold.getThresholdType() != ThresholdType.NO_ALERT) {
          relevantThreshold = true;
          RiskLevel thresholdRisk = RiskLevel.LOW;
          if (threshold.getThresholdType() == ThresholdType.ALERT_WHEN_HIGHER) {
            if (value > threshold.getHigh()) {
              thresholdRisk = RiskLevel.HIGH;
            } else if (value > threshold.getMedium() && value <= threshold.getHigh()) {
              thresholdRisk = RiskLevel.MEDIUM;
            } else {
              thresholdRisk = RiskLevel.LOW;
            }
          } else if (threshold.getThresholdType() == ThresholdType.ALERT_WHEN_LOWER) {
            if (value < threshold.getHigh()) {
              thresholdRisk = RiskLevel.HIGH;
            } else if (value < threshold.getMedium() && value >= threshold.getHigh()) {
              thresholdRisk = RiskLevel.MEDIUM;
            } else {
              thresholdRisk = RiskLevel.LOW;
            }
          }
          if (thresholdRisk.compareTo(newRisk) > 0) {
            newRisk = thresholdRisk;
          }
        }
      }
      if (relevantThreshold) {
        if (newRisk.compareTo(risk) < 0) {
          risk = newRisk;
        }
      }
    }
    BucketData bucketData = BucketData.Builder.aBucketData()
                                .withRisk(risk)
                                .withMetricType(metricDefinition.getMetricType())
                                .withOldData(oldSummary)
                                .withNewData(newSummary)
                                .build();
    return bucketData;
  }

  /**
   * Parses a single metric's records into the stats for that set of values.
   * @param metricDefinition The MetricDefinition for the metric in these records.
   * @param records The records.
   * @return A DataSummary containing the stats, the display value, and whether any data was missing.
   */
  public static DataSummary parsePartial(MetricDefinition metricDefinition, List<AppdynamicsMetricDataRecord> records) {
    if (records.size() == 0) {
      return null;
    }
    TreeMap<Long, Double> valueMap = new TreeMap<>();
    TreeMap<Long, List<Double>> tempValueMap = new TreeMap<>();
    HashSet<String> nodeSet = new HashSet<>();
    for (AppdynamicsMetricDataRecord record : records) {
      Long startTime = record.getStartTime();
      nodeSet.add(record.getNodeName());
      // this combines the values across all nodes for each time period
      if (tempValueMap.containsKey(startTime)) {
        tempValueMap.get(startTime).add(record.getValue());
      } else {
        tempValueMap.put(startTime, new ArrayList<>(Arrays.asList(record.getValue())));
      }
    }
    int nodeCount = nodeSet.size();
    for (Long key : tempValueMap.keySet()) {
      Stats tempStats = Stats.of(tempValueMap.get(key));
      if (metricDefinition.getMetricType() == MetricType.COUNT) {
        valueMap.put(key, tempStats.sum());
      } else if (metricDefinition.getMetricType() == MetricType.RATE) {
        valueMap.put(key, tempStats.mean());
      } else if (metricDefinition.getMetricType() == MetricType.PERCENTAGE) {
        valueMap.put(key, tempStats.mean());
      } else if (metricDefinition.getMetricType() == MetricType.TIME_MS
          || metricDefinition.getMetricType() == MetricType.TIME) {
        valueMap.put(key, tempStats.mean());
      }
    }
    Stats stats = Stats.of(valueMap.values());
    long startTime = valueMap.firstKey();
    long endTime = valueMap.lastKey() + TimeUnit.MINUTES.toMillis(1);

    /*
     * If the number of time values (the number of keys in valueMap) isn't equal to the number of minutes between
     * the start and end times, or if the number of time values multiplied by the number of nodes in the data isn't
     * equal to the number of records in the input list, there's at least one metric value missing.
     */
    boolean missingData = false;
    long expectedValueCount = TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);
    if ((valueMap.size() != expectedValueCount) || (valueMap.size() * nodeCount != records.size())) {
      missingData = true;
    }

    double value = 0;
    if (metricDefinition.getMetricType() == MetricType.COUNT) {
      value = stats.sum();
    } else if (metricDefinition.getMetricType() == MetricType.RATE) {
      value = stats.mean();
    } else if (metricDefinition.getMetricType() == MetricType.PERCENTAGE) {
      value = stats.mean();
    } else if (metricDefinition.getMetricType() == MetricType.TIME_MS
        || metricDefinition.getMetricType() == MetricType.TIME) {
      value = stats.mean();
    }
    return new BucketData.DataSummary(nodeCount, new ArrayList<>(nodeSet), stats, value, missingData);
  }

  public static MetricSummary.BTMetrics calculateOverallBTRisk(Map<String, BucketData> metricBucketDataMap) {
    RiskLevel risk = RiskLevel.LOW;
    List<String> messages = new ArrayList<>();
    for (String metric : metricBucketDataMap.keySet()) {
      BucketData bucketData = metricBucketDataMap.get(metric);
      if (bucketData.getRisk().compareTo(risk) < 0) {
        risk = bucketData.getRisk();
      }
    }
    for (String metric : metricBucketDataMap.keySet()) {
      BucketData bucketData = metricBucketDataMap.get(metric);
      if (bucketData.getRisk() == risk) {
        StringBuilder s = new StringBuilder();
        s.append(risk.name()).append(": ");
        s.append(metric);
        s.append(" (old value: ")
            .append(bucketData.getOldData() == null ? "<null>" : bucketData.getOldData().getValue());
        s.append(", new value: ")
            .append(bucketData.getNewData() == null ? "<null>" : bucketData.getNewData().getValue());
        s.append(")");
        messages.add(s.toString());
      }
    }

    Collections.sort(messages);
    return new MetricSummary.BTMetrics(risk, messages, metricBucketDataMap);
  }
}
