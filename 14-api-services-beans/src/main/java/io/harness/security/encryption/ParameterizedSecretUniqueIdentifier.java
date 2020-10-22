package io.harness.security.encryption;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Getter
@SuperBuilder
public class ParameterizedSecretUniqueIdentifier extends SecretUniqueIdentifier {
  private final Set<EncryptedDataParams> parameters;
}
