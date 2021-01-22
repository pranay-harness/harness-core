package io.harness.delegate.beans.connector.scm.gitlab;

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
@ApiModel("GitlabConnector")
public class GitlabConnectorDTO
    extends ConnectorConfigDTO implements ScmConnector, ExecutionCapabilityDemanderWithScope {
  @NotNull @JsonProperty("type") GitConnectionType connectionType;
  @NotNull String url;
  @Valid @NotNull GitlabAuthenticationDTO authentication;
  @Valid GitlabApiAccessDTO apiAccess;

  @Builder
  public GitlabConnectorDTO(GitConnectionType connectionType, String url, GitlabAuthenticationDTO authentication,
      GitlabApiAccessDTO apiAccess) {
    this.connectionType = connectionType;
    this.url = url;
    this.authentication = authentication;
    this.apiAccess = apiAccess;
  }

  @Override
  public DecryptableEntity getDecryptableEntity() {
    if (authentication.getAuthType() == GitAuthType.HTTP) {
      return ((GitlabHttpCredentialsDTO) authentication.getCredentials()).getHttpCredentialsSpec();
    } else {
      return ((GitlabSshCredentialsDTO) authentication.getCredentials()).getSpec();
    }
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return null;
  }
}
