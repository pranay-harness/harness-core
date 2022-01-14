package io.harness.delegate.beans.connector.scm.gitlab;

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
@ApiModel("GitlabKerberos")
@Schema(name = "GitlabKerberos",
    description = "This contains details of the Gitlab credentials Specs such as references of Keberos key")
public class GitlabKerberosDTO implements GitlabHttpCredentialsSpecDTO {
  @SecretReference @ApiModelProperty(dataType = "string") @NotNull SecretRefData kerberosKeyRef;
}
