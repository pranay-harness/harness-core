package io.harness.delegate.beans.connector.scm.github;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ExecutionCapabilityDemanderWithScope;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GithubConnector")
public class GithubConnectorDTO
    extends ConnectorConfigDTO implements ScmConnector, ExecutionCapabilityDemanderWithScope {
  @NotNull @JsonProperty("type") GitConnectionType connectionType;
  @NotNull String url;
  @Valid @NotNull GithubAuthenticationDTO authentication;
  @Valid GithubApiAccessDTO apiAccess;

  @Builder
  public GithubConnectorDTO(GitConnectionType connectionType, String url, GithubAuthenticationDTO authentication,
      GithubApiAccessDTO apiAccess) {
    this.connectionType = connectionType;
    this.url = url;
    this.authentication = authentication;
    this.apiAccess = apiAccess;
  }

  @Override
  public DecryptableEntity getDecryptableEntity() {
    if (authentication.getAuthType() == GitAuthType.HTTP) {
      return ((GithubHttpCredentialsDTO) authentication.getCredentials()).getHttpCredentialsSpec();
    } else {
      return ((GithubSshCredentialsDTO) authentication.getCredentials()).getSpec();
    }
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return null;
  }
}
