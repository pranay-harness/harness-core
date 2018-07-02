package software.wings.service.impl.analysis;

import lombok.Data;

import java.util.Map;

/**
 * Created by sriram_parthasarathy on 9/22/17.
 */
@Data
public class TimeSeriesMLMetricSummary {
  private String metric_name;
  private String metric_type;
  private String alert_type;
  private TimeSeriesMLDataSummary control;
  private TimeSeriesMLDataSummary test;
  private Map<String, TimeSeriesMLHostSummary> results;
  private double control_avg;
  private double test_avg;
  private int max_risk;
}
