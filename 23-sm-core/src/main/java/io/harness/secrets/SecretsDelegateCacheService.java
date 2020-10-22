package io.harness.secrets;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.SecretUniqueIdentifier;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.function.Function;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
public interface SecretsDelegateCacheService {
  char[] get(@NotNull SecretUniqueIdentifier key, @NotNull Function<SecretUniqueIdentifier, char[]> mappingFunction);
  void put(@NotNull SecretUniqueIdentifier key, @NotEmpty char[] value);
  void remove(@NotNull SecretUniqueIdentifier key);
}
