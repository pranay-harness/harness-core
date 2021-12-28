package io.harness.cvng.beans;

import io.harness.delegate.beans.connector.dynatrace.DynatraceConnectorDTO;
import io.harness.delegate.beans.cvng.dynatrace.DynatraceUtils;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class DynatraceDataCollectionInfo extends TimeSeriesDataCollectionInfo<DynatraceConnectorDTO> {

    private List<Object> metricDefinitions;

    @Override
    public Map<String, Object> getDslEnvVariables(DynatraceConnectorDTO connectorConfigDTO) {
        return null;
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
        private String entityId;
    }
}
