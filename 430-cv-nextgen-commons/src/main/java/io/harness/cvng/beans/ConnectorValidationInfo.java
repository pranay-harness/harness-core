package io.harness.cvng.beans;

import io.harness.cvng.beans.appd.AppDynamicsConnectorValidationInfo;
import io.harness.cvng.beans.newrelic.NewRelicConnectorValidationInfo;
import io.harness.cvng.beans.prometheus.PrometheusConnectorValidationInfo;
import io.harness.cvng.beans.splunk.SplunkConnectorValidationInfo;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.delegate.beans.connector.prometheusconnector.PrometheusConnectorDTO;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import lombok.Data;

@Data
public abstract class ConnectorValidationInfo<T extends ConnectorConfigDTO> {
  protected T connectorConfigDTO;
  public static ConnectorValidationInfo getConnectorValidationInfo(ConnectorConfigDTO connectorConfigDTO) {
    ConnectorValidationInfo connectorValidationInfo;
    if (connectorConfigDTO instanceof AppDynamicsConnectorDTO) {
      connectorValidationInfo = AppDynamicsConnectorValidationInfo.builder().build();
    } else if (connectorConfigDTO instanceof SplunkConnectorDTO) {
      connectorValidationInfo = SplunkConnectorValidationInfo.builder().build();
    } else if (connectorConfigDTO instanceof NewRelicConnectorDTO) {
      connectorValidationInfo = NewRelicConnectorValidationInfo.builder().build();
    } else if (connectorConfigDTO instanceof PrometheusConnectorDTO) {
      connectorValidationInfo = PrometheusConnectorValidationInfo.builder().build();
    } else {
      throw new IllegalStateException(
          "Class: " + connectorConfigDTO.getClass().getSimpleName() + " does not have ValidationInfo object");
    }
    connectorValidationInfo.setConnectorConfigDTO(connectorConfigDTO);
    return connectorValidationInfo;
  }

  public abstract String getConnectionValidationDSL();

  public abstract String getBaseUrl();

  public abstract Map<String, String> collectionHeaders();

  public Map<String, String> collectionParams() {
    return Collections.emptyMap();
  }

  public Map<String, Object> getDslEnvVariables() {
    return Collections.emptyMap();
  }

  public Instant getEndTime(Instant currentTime) {
    return currentTime;
  }
  public Instant getStartTime(Instant currentTime) {
    return currentTime.minus(Duration.ofMinutes(1));
  }

  protected static String readDSL(String fileName, Class clazz) {
    try {
      return Resources.toString(clazz.getResource(fileName), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
