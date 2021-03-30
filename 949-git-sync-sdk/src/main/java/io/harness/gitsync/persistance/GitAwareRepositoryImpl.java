package io.harness.gitsync.persistance;

import io.harness.gitsync.beans.NGDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.result.DeleteResult;
import lombok.AllArgsConstructor;
import org.springframework.util.Assert;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class GitAwareRepositoryImpl<T extends GitSyncableEntity, Y extends NGDTO, ID>
    implements GitAwareRepository<T, Y, ID> {
  private final GitAwarePersistence gitAwarePersistence;

  @Override
  public T save(T entity, Y yaml) {
    Assert.notNull(entity, "Entity must not be null!");
    return (T) gitAwarePersistence.save(entity, yaml);
  }

  @Override
  public DeleteResult delete(T entity, Y yaml) {
    Assert.notNull(entity, "The given entity must not be null!");
    return gitAwarePersistence.remove(entity, yaml);
  }
}
