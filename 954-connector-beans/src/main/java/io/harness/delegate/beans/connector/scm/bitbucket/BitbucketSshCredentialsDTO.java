package io.harness.delegate.beans.connector.scm.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("BitbucketSshCredentials")
public class BitbucketSshCredentialsDTO implements BitbucketCredentialsDTO {
  @Valid @NotNull BitbucketSshCredentialsSpecDTO spec;
}
