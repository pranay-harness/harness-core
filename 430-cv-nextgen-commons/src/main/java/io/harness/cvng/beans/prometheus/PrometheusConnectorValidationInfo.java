package io.harness.cvng.beans.prometheus;

import io.harness.cvng.beans.ConnectorValidationInfo;
import io.harness.delegate.beans.connector.prometheusconnector.PrometheusConnectorDTO;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

@Data
@Builder
public class PrometheusConnectorValidationInfo extends ConnectorValidationInfo<PrometheusConnectorDTO> {
    private static final String DSL =
            readDSL("prometheus-validation.datacollection", PrometheusConnectorValidationInfo.class);
    @Override
    public String getConnectionValidationDSL() {
        return DSL;
    }

    @Override
    public String getBaseUrl() {
        return connectorConfigDTO.getUrl();
    }

    @Override
    public Map<String, String> collectionHeaders() {
        return Collections.emptyMap();
    }

}
