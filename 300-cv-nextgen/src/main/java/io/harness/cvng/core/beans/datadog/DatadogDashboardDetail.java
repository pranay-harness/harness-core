package io.harness.cvng.core.beans.datadog;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;
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
