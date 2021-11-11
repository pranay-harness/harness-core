package io.harness.cvng.beans;

import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;
import io.harness.delegate.beans.cvng.datadog.DatadogUtils;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
public class DatadogMetricsDataCollectionInfo extends TimeSeriesDataCollectionInfo<DatadogConnectorDTO> {
    private String groupName;
    private List<MetricCollectionInfo> metricDefinitions;

    @Override
    public Map<String, Object> getDslEnvVariables(DatadogConnectorDTO connectorConfigDTO) {
        Map<String, Object> dslEnvVariables = new HashMap<>();
        List<String> queries = metricDefinitions.stream()
                .map(metricCollectionInfo -> {
                    if (isCollectHostData() && metricCollectionInfo.getServiceInstanceIdentifierTag() != null &&
                            metricCollectionInfo.getGroupingQuery() != null) {
                        return metricCollectionInfo.getGroupingQuery();
                    }
                    return metricCollectionInfo.getQuery();
                })
                .collect(Collectors.toList());
        dslEnvVariables.put("queries", queries);
        dslEnvVariables.put("groupName", groupName);
        return dslEnvVariables;
    }

    @Override
    public String getBaseUrl(DatadogConnectorDTO connectorConfigDTO) {
        return connectorConfigDTO.getUrl();
    }

    @Override
    public Map<String, String> collectionHeaders(DatadogConnectorDTO connectorConfigDTO) {
        return DatadogUtils.collectionHeaders(connectorConfigDTO);
    }

    @Override
    public Map<String, String> collectionParams(DatadogConnectorDTO connectorConfigDTO) {
        return Collections.emptyMap();
    }

    @Data
    @Builder
    public static class MetricCollectionInfo {
        private String query;
        private String groupingQuery;
        private String metricName;
        private String metric;
        private String serviceInstanceIdentifierTag;

    }
}
