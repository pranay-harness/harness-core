package io.harness.security.encryption;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode
public class SecretUniqueIdentifier {
  String kmsId;
}
