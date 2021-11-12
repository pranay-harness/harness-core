package io.harness.cvng.core.beans.datadog;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
@Value
@Builder
public class DatadogDashboardDetail {
  String widgetName;
  List<DataSet> dataSets;

  @Data
  @Builder
  public static class DataSet {
    String name;
    String query;
  }
}
