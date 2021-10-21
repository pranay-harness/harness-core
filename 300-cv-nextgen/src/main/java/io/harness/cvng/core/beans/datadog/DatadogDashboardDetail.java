package io.harness.cvng.core.beans.datadog;

import lombok.Builder;
import lombok.Data;

import java.util.List;
@Data
@Builder
public class DatadogDashboardDetail {

    String widgetName;
    List<DataSet> dataSets;

    @Data
    @Builder
    public static class DataSet {
        private List<DatadogQuery> queries;
    }
    @Data
    @Builder
    public static class DatadogQuery {
        String name;
        String query;
    }
}
