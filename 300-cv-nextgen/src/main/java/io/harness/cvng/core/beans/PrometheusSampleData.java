package io.harness.cvng.core.beans;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Value
@Builder
public class PrometheusSampleData {
  Map<String, String> metricDetails;
  List<List<Double>> data;

  @Data
  @Builder
  public static class DataPoint {
    long timestamp;
    double value;

    public void setValue(String val) {
      this.value = Double.valueOf(val);
    }
  }
}
