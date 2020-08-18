package io.harness.delegate.beans.connector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = AppDynamicsConnectorDTO.class, name = "AppDynamics") })
public abstract class ConnectorConfigDTO {}
