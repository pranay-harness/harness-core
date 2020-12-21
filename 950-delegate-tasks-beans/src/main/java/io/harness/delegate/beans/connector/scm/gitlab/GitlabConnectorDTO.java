package io.harness.delegate.beans.connector.scm.gitlab;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.GitConnectionType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GitlabConnector")
public class GitlabConnectorDTO extends ConnectorConfigDTO {
  @NotNull @JsonProperty("type") GitConnectionType connectionType;
  @NotNull String url;
  @Valid @NotNull GitlabAuthenticationDTO authentication;
  @Valid GitlabApiAccessDTO apiAccess;

  @Override
  public DecryptableEntity getDecryptableEntity() {
    return null;
  }
}
