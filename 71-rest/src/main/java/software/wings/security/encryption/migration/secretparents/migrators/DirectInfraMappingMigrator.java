package software.wings.security.encryption.migration.secretparents.migrators;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.InfrastructureMapping;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.migration.secretparents.SecretsMigrator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class DirectInfraMappingMigrator implements SecretsMigrator<InfrastructureMapping> {
  private WingsPersistence wingsPersistence;

  @Inject
  public DirectInfraMappingMigrator(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public List<InfrastructureMapping> getParents(Set<String> parentIds) {
    return parentIds.stream()
        .map(parentId -> wingsPersistence.get(InfrastructureMapping.class, parentId))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public Optional<EncryptedDataParent> buildEncryptedDataParent(
      @NotNull String secretId, @NotNull InfrastructureMapping parent) {
    return Optional.of(
        new EncryptedDataParent(parent.getUuid(), parent.getSettingType(), parent.getSettingType().toString()));
  }
}
