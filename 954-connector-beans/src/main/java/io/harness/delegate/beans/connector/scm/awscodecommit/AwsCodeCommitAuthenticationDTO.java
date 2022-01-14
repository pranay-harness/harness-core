package io.harness.delegate.beans.connector.scm.awscodecommit;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.SourceCodeManagerAuthentication;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(HarnessTeam.CI)
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("AwsCodeCommitAuthenticationDTO")
@Schema(name = "AwsCodeCommitAuthentication", description = "This contains details of the AWS Code Commit credentials")
public class AwsCodeCommitAuthenticationDTO implements SourceCodeManagerAuthentication {
  @NotNull @JsonProperty("type") AwsCodeCommitAuthType authType;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  @NotNull
  AwsCodeCommitCredentialsDTO credentials;

  @Builder
  public AwsCodeCommitAuthenticationDTO(AwsCodeCommitAuthType authType, AwsCodeCommitCredentialsDTO credentials) {
    this.authType = authType;
    this.credentials = credentials;
  }
}