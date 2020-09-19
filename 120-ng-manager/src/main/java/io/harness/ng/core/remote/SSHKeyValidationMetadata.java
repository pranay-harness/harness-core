package io.harness.ng.core.remote;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SSHKey")
public class SSHKeyValidationMetadata extends SecretValidationMetaData {
  private String host;
}
