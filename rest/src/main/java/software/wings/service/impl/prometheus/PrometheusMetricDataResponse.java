package software.wings.service.impl.prometheus;

import com.google.common.collect.TreeBasedTable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.MetricCollectionResponse;
import software.wings.sm.StateType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 3/16/18.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrometheusMetricDataResponse implements MetricCollectionResponse {
  private String status;
  private PrometheusMetricData data;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PrometheusMetricData {
    private String resultType;
    private List<PrometheusMetricDataResult> result;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PrometheusMetricDataResult {
    private PrometheusMetric metric;
    private List<Object> value;
    private List<List<Object>> values;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PrometheusMetric {
    private String __name__;
    private String instance;
    private String job;
  }

  @Override
  public TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricRecords(String transactionName,
      String metricName, String appId, String workflowId, String workflowExecutionId, String stateExecutionId,
      String serviceId, String host, String groupName, int collectionMinute) {
    TreeBasedTable<String, Long, NewRelicMetricDataRecord> rv = TreeBasedTable.create();
    if (!status.equals("success")) {
      return rv;
    }

    if (data == null) {
      return rv;
    }

    if (data.getResult() == null) {
      return rv;
    }

    for (PrometheusMetricDataResult dataResult : data.getResult()) {
      if (dataResult.getMetric() == null) {
        continue;
      }

      if (dataResult.getValues() == null) {
        continue;
      }

      for (List<Object> metricValues : dataResult.getValues()) {
        long timeStamp = (int) metricValues.get(0) * TimeUnit.SECONDS.toMillis(1);
        Double value = Double.valueOf((String) metricValues.get(1));

        NewRelicMetricDataRecord metricDataRecord = rv.get(transactionName, timeStamp);
        if (metricDataRecord == null) {
          metricDataRecord = NewRelicMetricDataRecord.builder()
                                 .name(transactionName)
                                 .appId(appId)
                                 .workflowId(workflowId)
                                 .workflowExecutionId(workflowExecutionId)
                                 .stateExecutionId(stateExecutionId)
                                 .serviceId(serviceId)
                                 .dataCollectionMinute(collectionMinute)
                                 .timeStamp(timeStamp)
                                 .stateType(StateType.PROMETHEUS)
                                 .host(host)
                                 .groupName(groupName)
                                 .build();
          metricDataRecord.setAppId(appId);
          rv.put(transactionName, timeStamp, metricDataRecord);
        }

        Map<String, Double> values = metricDataRecord.getValues();
        if (values == null) {
          values = new HashMap<>();
          metricDataRecord.setValues(values);
        }

        values.put(metricName, value);
      }
    }
    return rv;
  }
}
