package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import org.springframework.util.Assert;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitAwareRepositoryImpl<T extends GitSyncableEntity, Y extends YamlDTO>
    implements GitAwareRepository<T, Y> {
  private final GitAwarePersistence<T, Y> gitAwarePersistence;

  @Override
  public T save(T entity, Y yaml) {
    Assert.notNull(entity, "Entity must not be null!");
    return gitAwarePersistence.save(entity, yaml);
  }
}
