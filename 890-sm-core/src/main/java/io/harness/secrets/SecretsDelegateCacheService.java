package io.harness.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.SecretUniqueIdentifier;

import java.util.function.Function;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface SecretsDelegateCacheService {
  char[] get(@NotNull SecretUniqueIdentifier key, @NotNull Function<SecretUniqueIdentifier, char[]> mappingFunction);
  void put(@NotNull SecretUniqueIdentifier key, @NotEmpty char[] value);
  void remove(@NotNull SecretUniqueIdentifier key);
}
