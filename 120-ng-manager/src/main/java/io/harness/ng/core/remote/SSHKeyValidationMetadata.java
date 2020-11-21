package io.harness.ng.core.remote;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SSHKey")
public class SSHKeyValidationMetadata extends SecretValidationMetaData {
  @NotNull private String host;

  public void setHost(String host) {
    this.host = host.trim();
  }
}
