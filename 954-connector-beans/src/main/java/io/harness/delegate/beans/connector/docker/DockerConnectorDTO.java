package io.harness.delegate.beans.connector.docker;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DockerConnectorDTO extends ConnectorConfigDTO {
  @NotNull String dockerRegistryUrl;
  @NotNull DockerRegistryProviderType providerType;
  @Valid DockerAuthenticationDTO auth;

  @Override
  public DecryptableEntity getDecryptableEntity() {
    return auth.getCredentials();
  }
}
