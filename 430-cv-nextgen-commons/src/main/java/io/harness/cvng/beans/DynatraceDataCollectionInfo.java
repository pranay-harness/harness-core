package io.harness.cvng.beans;

import com.mongodb.lang.Nullable;
import io.harness.delegate.beans.connector.dynatrace.DynatraceConnectorDTO;
import io.harness.delegate.beans.cvng.dynatrace.DynatraceUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections4.CollectionUtils;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class DynatraceDataCollectionInfo extends TimeSeriesDataCollectionInfo<DynatraceConnectorDTO> {
    private static final String METRIC_NAME_PARAM = "metricName";
    private static final String QUERY_SELECTOR_PARAM = "querySelector";
    private static final String GROUP_NAME_PARAM = "groupName";
    private static final String ENTITY_ID_PARAM = "entityId";
    private static final String ENTITY_SELECTOR_PARAM = "entitySelector";

    private String groupName;
    private String serviceId;
    @Nullable
    private List<MetricCollectionInfo> customMetrics;
    @Nullable
    private MetricPackDTO metricPack;

    @Override
    public Map<String, Object> getDslEnvVariables(DynatraceConnectorDTO connectorConfigDTO) {
        Map<String, Object> dslEnvVariables = new HashMap<>();
        dslEnvVariables.put(ENTITY_ID_PARAM, serviceId);
        dslEnvVariables.put("resolution", "1m");
        dslEnvVariables.put(GROUP_NAME_PARAM, groupName != null ? groupName : metricPack.getIdentifier());
        dslEnvVariables.put("host", isCollectHostData() ? "dynatrace-placeholder-host" : null);

        if (metricPack != null) {
            List<Map<String, String>> queryData = CollectionUtils.emptyIfNull(metricPack.getMetrics())
                    .stream()
                    .map(metricDefinitionDTO -> {
                        Map<String, String> metricMap = new HashMap<>();
                        metricMap.put(METRIC_NAME_PARAM, metricDefinitionDTO.getName());
                        metricMap.put(QUERY_SELECTOR_PARAM, metricDefinitionDTO.getPath());
                        return metricMap;
                    })
                    .collect(Collectors.toList());
            dslEnvVariables.put("metricsToValidate", queryData);
        } else if (customMetrics != null) {
            List<Map<String, String>> queryData = CollectionUtils.emptyIfNull(customMetrics)
                    .stream()
                    .map(metricDefinitionDTO -> {
                        Map<String, String> metricMap = new HashMap<>();
                        metricMap.put(METRIC_NAME_PARAM, metricDefinitionDTO.getMetricName());
                        metricMap.put(QUERY_SELECTOR_PARAM, metricDefinitionDTO.getQuery());
                        return metricMap;
                    })
                    .collect(Collectors.toList());
            dslEnvVariables.put("metricsToValidate", queryData);
        }
        return dslEnvVariables;
    }

    @Override
    public String getBaseUrl(DynatraceConnectorDTO dynatraceConnectorDTO) {
        return dynatraceConnectorDTO.getUrl();
    }

    @Override
    public Map<String, String> collectionHeaders(DynatraceConnectorDTO dynatraceConnectorDTO) {
        return DynatraceUtils.collectionHeaders(dynatraceConnectorDTO);
    }

    @Override
    public Map<String, String> collectionParams(DynatraceConnectorDTO dynatraceConnectorDTO) {
        return Collections.emptyMap();
    }

    @Data
    @Builder
    public static class MetricCollectionInfo {
        String identifier;
        String metricName;
        String query;
    }
}
