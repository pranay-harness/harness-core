package io.harness.delegate.beans.connector.scm.github;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel("GithubConnector")
@OwnedBy(HarnessTeam.DX)
public class GithubConnectorDTO extends ConnectorConfigDTO implements ScmConnector, DelegateSelectable {
  @NotNull @JsonProperty("type") GitConnectionType connectionType;
  @NotBlank @NotNull @ApiModelProperty("Url of the github repository.") String url;
  @Valid @NotNull GithubAuthenticationDTO authentication;
  @Valid GithubApiAccessDTO apiAccess;
  Set<String> delegateSelectors;
  @Builder
  public GithubConnectorDTO(GitConnectionType connectionType, String url, GithubAuthenticationDTO authentication,
      GithubApiAccessDTO apiAccess, Set<String> delegateSelectors) {
    this.connectionType = connectionType;
    this.url = url;
    this.authentication = authentication;
    this.apiAccess = apiAccess;
    this.delegateSelectors = delegateSelectors;
  }

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    List<DecryptableEntity> decryptableEntities = new ArrayList<>();
    if (authentication.getAuthType() == GitAuthType.HTTP) {
      GithubHttpCredentialsSpecDTO httpCredentialsSpec =
          ((GithubHttpCredentialsDTO) authentication.getCredentials()).getHttpCredentialsSpec();
      if (httpCredentialsSpec != null) {
        decryptableEntities.add(httpCredentialsSpec);
      }
    } else {
      GithubSshCredentialsDTO sshCredential = (GithubSshCredentialsDTO) authentication.getCredentials();
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
