package software.wings.security.encryption.migration.secretparents;

import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import software.wings.security.encryption.EncryptedDataParent;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SecretsMigrator<T extends PersistentEntity & UuidAware> {
  List<T> getParents(Set<String> parentIds);
  Optional<EncryptedDataParent> buildEncryptedDataParent(String secretId, T parent);
}
