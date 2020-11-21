package io.harness.security.encryption;

import java.util.Set;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class ParameterizedSecretUniqueIdentifier extends SecretUniqueIdentifier {
  private final Set<EncryptedDataParams> parameters;
}
