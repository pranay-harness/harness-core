package io.harness.delegate.beans.connector.nexusconnector;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("NexusConnector")
public class NexusConnectorDTO extends ConnectorConfigDTO {
  @NotNull String nexusServerUrl;
  @NotNull String version;
  @Valid NexusAuthenticationDTO auth;
}
