package io.harness.ng.core.remote;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SSHKey")
public class SSHKeyValidationMetadata extends SecretValidationMetaData {
  private String host;
}
