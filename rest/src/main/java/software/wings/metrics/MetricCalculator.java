package software.wings.metrics;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.math.Stats;

import software.wings.metrics.BucketData.DataSummary;
import software.wings.metrics.MetricDefinition.ThresholdType;
import software.wings.metrics.appdynamics.AppdynamicsMetricDefinition;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricDataRecord;

import java.util.ArrayList;
import java.util.Arrays;
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
        } else {
          MetricType metricType = MetricType.COUNT;
          if (record.getMetricName().endsWith("(ms)")) {
            metricType = MetricType.TIME;
          }
          metricDefinition = AppdynamicsMetricDefinition.Builder.anAppdynamicsMetricDefinition()
                                 .withAccountId(record.getAccountId())
                                 .withAppdynamicsAppId(record.getAppdAppId())
                                 .withMetricId(String.valueOf(record.getMetricId()))
                                 .withMetricName(record.getMetricName())
                                 .withMetricType(metricType)
                                 .withMediumThreshold(1.0)
                                 .withHighThreshold(2.0)
                                 .withThresholdType(ThresholdType.ALERT_WHEN_HIGHER)
                                 .build();
        }
        metricData.put(metricDefinition, record);
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
      if (!metricDataMap.containsKey("Total Calls") && metricDataMap.containsKey("Calls per Minute")) {
        BucketData bucketData = metricDataMap.get("Calls per Minute");
        BucketData callsBucket = new BucketData();
        callsBucket.setRisk(bucketData.getRisk());
        if (bucketData.getOldData() != null) {
          DataSummary oldDataSummary = bucketData.getOldData();
          callsBucket.setOldData(new BucketData.DataSummary(oldDataSummary.getNodeCount(), oldDataSummary.getNodeList(),
              oldDataSummary.getStats(), oldDataSummary.getDisplayValue(), oldDataSummary.isMissingData()));
          callsBucket.getOldData().setDisplayValue(String.valueOf(callsBucket.getOldData().getStats().sum()
              * (TimeUnit.MILLISECONDS.toMinutes(endTimeMillis - startTimeMillis) + 1)));
        }
        if (bucketData.getNewData() != null) {
          DataSummary newDataSummary = bucketData.getOldData();
          callsBucket.setNewData(new BucketData.DataSummary(newDataSummary.getNodeCount(), newDataSummary.getNodeList(),
              newDataSummary.getStats(), newDataSummary.getDisplayValue(), newDataSummary.isMissingData()));
          callsBucket.getNewData().setDisplayValue(String.valueOf(callsBucket.getNewData().getStats().sum()
              * (TimeUnit.MILLISECONDS.toMinutes(endTimeMillis - startTimeMillis) + 1)));
        }
        metricDataMap.put("Total Calls", callsBucket);
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

    MetricSummary metricSummary = MetricSummary.Builder.aMetricSummary()
                                      .withAccountId(accountId)
                                      .withBtMetricsMap(btMetricDataMap)
                                      .withRiskLevel(risk)
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
    RiskLevel risk = RiskLevel.LOW;
    // default thresholds: 1-2x = medium, >2x high
    double ratio = 0.0;
    if (newSummary != null && oldSummary != null) {
      if (metricDefinition.getMetricType() == MetricType.COUNT) {
        ratio = (newSummary.getStats().sum() / newSummary.getNodeCount())
            / (oldSummary.getStats().sum() / oldSummary.getNodeCount());
      } else if (metricDefinition.getMetricType() == MetricType.PERCENTAGE
          || metricDefinition.getMetricType() == MetricType.TIME) {
        ratio = newSummary.getStats().mean() / oldSummary.getStats().mean();
      }
      if (metricDefinition.getThresholdType() == ThresholdType.ALERT_WHEN_HIGHER) {
        if (ratio > metricDefinition.getHighThreshold()) {
          risk = RiskLevel.HIGH;
        } else if (ratio > metricDefinition.getMediumThreshold() && ratio <= metricDefinition.getHighThreshold()) {
          risk = RiskLevel.MEDIUM;
        }
      } else if (metricDefinition.getThresholdType() == ThresholdType.ALERT_WHEN_LOWER) {
        if (ratio < metricDefinition.getHighThreshold()) {
          risk = RiskLevel.HIGH;
        } else if (ratio < metricDefinition.getMediumThreshold() && ratio >= metricDefinition.getHighThreshold()) {
          risk = RiskLevel.MEDIUM;
        }
      }
    }
    BucketData bucketData =
        BucketData.Builder.aBucketData().withRisk(risk).withOldData(oldSummary).withNewData(newSummary).build();
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
      } else if (metricDefinition.getMetricType() == MetricType.PERCENTAGE) {
        valueMap.put(key, tempStats.mean());
      } else if (metricDefinition.getMetricType() == MetricType.TIME) {
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

    String displayValue = "";
    if (metricDefinition.getMetricType() == MetricType.COUNT) {
      displayValue = String.valueOf(stats.sum());
    } else if (metricDefinition.getMetricType() == MetricType.PERCENTAGE) {
      displayValue = String.valueOf(stats.mean());
    } else if (metricDefinition.getMetricType() == MetricType.TIME) {
      displayValue = String.valueOf(stats.mean());
    }
    return new BucketData.DataSummary(nodeCount, new ArrayList<>(nodeSet), stats, displayValue, missingData);
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
            .append(bucketData.getOldData() == null ? "<null>" : bucketData.getOldData().getDisplayValue());
        s.append(", new value: ")
            .append(bucketData.getNewData() == null ? "<null>" : bucketData.getNewData().getDisplayValue());
        s.append(")");
        messages.add(s.toString());
      }
    }
    return new MetricSummary.BTMetrics(risk, messages, metricBucketDataMap);
  }
}
