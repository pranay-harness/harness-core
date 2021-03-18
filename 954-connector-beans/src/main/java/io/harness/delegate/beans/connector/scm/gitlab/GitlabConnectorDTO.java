package io.harness.delegate.beans.connector.scm.gitlab;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GitlabConnector")
public class GitlabConnectorDTO extends ConnectorConfigDTO implements ScmConnector, DelegateSelectable {
  @NotNull @JsonProperty("type") GitConnectionType connectionType;
  @NotNull @NotBlank String url;
  @Valid @NotNull GitlabAuthenticationDTO authentication;
  @Valid GitlabApiAccessDTO apiAccess;
  Set<String> delegateSelectors;
  @Builder
  public GitlabConnectorDTO(GitConnectionType connectionType, String url, GitlabAuthenticationDTO authentication,
      GitlabApiAccessDTO apiAccess) {
    this.connectionType = connectionType;
    this.url = url;
    this.authentication = authentication;
    this.apiAccess = apiAccess;
  }

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    List<DecryptableEntity> decryptableEntities = new ArrayList<>();
    if (authentication.getAuthType() == GitAuthType.HTTP) {
      GitlabHttpCredentialsSpecDTO httpCredentialsSpec =
          ((GitlabHttpCredentialsDTO) authentication.getCredentials()).getHttpCredentialsSpec();
      if (httpCredentialsSpec != null) {
        decryptableEntities.add(httpCredentialsSpec);
      }
    } else {
      GitlabSshCredentialsDTO sshCredential = (GitlabSshCredentialsDTO) authentication.getCredentials();
      if (sshCredential != null) {
        decryptableEntities.add(sshCredential);
      }
    }
    if (apiAccess != null && apiAccess.getSpec() != null) {
      decryptableEntities.add(apiAccess.getSpec());
    }
    return decryptableEntities;
  }
}
