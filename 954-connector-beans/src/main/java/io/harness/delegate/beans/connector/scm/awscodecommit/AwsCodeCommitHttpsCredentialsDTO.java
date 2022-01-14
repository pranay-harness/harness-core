package io.harness.delegate.beans.connector.scm.awscodecommit;

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

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("AwsCodeCommitHttpsCredentialsDTO")
@Schema(name = "AwsCodeCommitHttpsCredentials",
    description = "This contains details of the AWS Code Commit credentials used via HTTPS connections")
public class AwsCodeCommitHttpsCredentialsDTO implements AwsCodeCommitCredentialsDTO {
  @NotNull AwsCodeCommitHttpsAuthType type;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  @NotNull
  AwsCodeCommitHttpsCredentialsSpecDTO httpCredentialsSpec;

  @Builder
  public AwsCodeCommitHttpsCredentialsDTO(
      AwsCodeCommitHttpsAuthType type, AwsCodeCommitHttpsCredentialsSpecDTO httpCredentialsSpec) {
    this.type = type;
    this.httpCredentialsSpec = httpCredentialsSpec;
  }
}
