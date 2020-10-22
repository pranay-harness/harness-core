package io.harness.security.encryption;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class InlineSecretUniqueIdentifier extends SecretUniqueIdentifier {
  private final String encryptionKey;
}
