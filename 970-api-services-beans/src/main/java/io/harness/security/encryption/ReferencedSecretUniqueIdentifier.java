package io.harness.security.encryption;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ReferencedSecretUniqueIdentifier extends SecretUniqueIdentifier {
  private final String path;
}
