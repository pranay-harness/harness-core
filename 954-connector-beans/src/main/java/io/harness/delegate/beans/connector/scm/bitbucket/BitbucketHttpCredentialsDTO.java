package io.harness.delegate.beans.connector.scm.bitbucket;

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
@ApiModel("BitbucketHttpCredentials")
@Schema(name = "BitbucketHttpCredentials",
    description = "This contains details of the Bitbucket credentials used via HTTP connections")
public class BitbucketHttpCredentialsDTO implements BitbucketCredentialsDTO {
  @NotNull BitbucketHttpAuthenticationType type;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  @NotNull
  BitbucketHttpCredentialsSpecDTO httpCredentialsSpec;

  @Builder
  public BitbucketHttpCredentialsDTO(
      BitbucketHttpAuthenticationType type, BitbucketHttpCredentialsSpecDTO httpCredentialsSpec) {
    this.type = type;
    this.httpCredentialsSpec = httpCredentialsSpec;
  }
}
