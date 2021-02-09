package io.harness.delegate.beans.connector.artifactoryconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("ArtifactoryConnector")
public class ArtifactoryConnectorDTO extends ConnectorConfigDTO {
  @NotNull @NotBlank String artifactoryServerUrl;
  @Valid ArtifactoryAuthenticationDTO auth;

  @Override
  public DecryptableEntity getDecryptableEntity() {
    return auth.getCredentials();
  }
}
