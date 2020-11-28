package io.harness.security.encryption;

import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ParameterizedSecretUniqueIdentifier extends SecretUniqueIdentifier {
  private final Set<EncryptedDataParams> parameters;
}
