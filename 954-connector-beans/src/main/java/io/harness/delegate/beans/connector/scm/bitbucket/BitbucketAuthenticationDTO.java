package io.harness.delegate.beans.connector.scm.bitbucket;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.SourceCodeManagerAuthentication;
import io.harness.delegate.beans.connector.scm.GitAuthType;

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

@OwnedBy(HarnessTeam.DX)
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("BitbucketAuthentication")
@Schema(name = "BitbucketAuthentication",
    description = "This contains details of the information needed for Bitbucket access")
public class BitbucketAuthenticationDTO implements SourceCodeManagerAuthentication {
  @NotNull @JsonProperty("type") GitAuthType authType;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  @NotNull
  BitbucketCredentialsDTO credentials;

  @Builder
  public BitbucketAuthenticationDTO(GitAuthType authType, BitbucketCredentialsDTO credentials) {
    this.authType = authType;
    this.credentials = credentials;
  }
}
