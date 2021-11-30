package io.harness.cvng.core.beans.datadog;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
@Value
@Builder
public class DatadogDashboardDetail {
  String widgetName;
  List<DatadogDataSet> dataSets;

  @Data
  @Builder
  public static class DatadogDataSet {
    String name;
    String query;
  }
}
