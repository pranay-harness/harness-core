package io.harness.security.encryption;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class InlineSecretUniqueIdentifier extends SecretUniqueIdentifier {
  private final String encryptionKey;
}
