package io.harness.delegate.beans.connector.scm.bitbucket;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
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
@ApiModel("BitbucketConnector")
public class BitbucketConnectorDTO extends ConnectorConfigDTO implements ScmConnector {
  @NotNull @JsonProperty("type") GitConnectionType connectionType;
  @NotNull @NotBlank String url;
  @Valid @NotNull BitbucketAuthenticationDTO authentication;
  @Valid BitbucketApiAccessDTO apiAccess;

  @Builder
  public BitbucketConnectorDTO(GitConnectionType connectionType, String url, BitbucketAuthenticationDTO authentication,
      BitbucketApiAccessDTO apiAccess) {
    this.connectionType = connectionType;
    this.url = url;
    this.authentication = authentication;
    this.apiAccess = apiAccess;
  }

  @Override
  public DecryptableEntity getDecryptableEntity() {
    if (authentication.getAuthType() == GitAuthType.HTTP) {
      return ((BitbucketHttpCredentialsDTO) authentication.getCredentials()).getHttpCredentialsSpec();
    } else {
      return (BitbucketSshCredentialsDTO) authentication.getCredentials();
    }
  }
}
