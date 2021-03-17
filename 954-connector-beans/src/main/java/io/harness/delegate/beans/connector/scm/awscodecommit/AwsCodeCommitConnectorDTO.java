package io.harness.delegate.beans.connector.scm.awscodecommit;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.ScmConnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.util.ArrayList;
import java.util.List;
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
@ApiModel("AwsCodeCommitConnectorDTO")
public class AwsCodeCommitConnectorDTO extends ConnectorConfigDTO implements ScmConnector {
  @NotNull @JsonProperty("type") AwsCodeCommitUrlType urlType;
  @NotNull @NotBlank String url;
  @Valid @NotNull AwsCodeCommitAuthenticationDTO authentication;

  @Builder
  public AwsCodeCommitConnectorDTO(
      AwsCodeCommitUrlType urlType, String url, AwsCodeCommitAuthenticationDTO authentication) {
    this.urlType = urlType;
    this.url = url;
    this.authentication = authentication;
  }

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    List<DecryptableEntity> decryptableEntities = new ArrayList<>();
    if (authentication.getAuthType() == AwsCodeCommitAuthType.HTTPS) {
      AwsCodeCommitHttpsCredentialsSpecDTO httpCredentialsSpec =
          ((AwsCodeCommitHttpsCredentialsDTO) authentication.getCredentials()).getHttpCredentialsSpec();
      if (httpCredentialsSpec != null) {
        decryptableEntities.add(httpCredentialsSpec);
      }
    }
    return decryptableEntities;
  }
}
