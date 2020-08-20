package io.harness.delegate.beans.connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AppDynamicsConnectorDTO.class, name = "AppDynamics")
  , @JsonSubTypes.Type(value = SplunkConnectorDTO.class, name = "Splunk")
})
public abstract class ConnectorConfigDTO {}
