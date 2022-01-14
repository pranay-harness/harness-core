package io.harness.delegate.beans.connector.scm.gitlab;

import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GitlabSshCredentials")
@Schema(name = "GitlabSshCredentials",
    description = "This contains details of the Gitlab credentials used via SSH connections")
public class GitlabSshCredentialsDTO implements GitlabCredentialsDTO, DecryptableEntity {
  @NotNull @SecretReference @ApiModelProperty(dataType = "string") SecretRefData sshKeyRef;
}
