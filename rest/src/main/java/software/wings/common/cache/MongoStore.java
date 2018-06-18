package software.wings.common.cache;

import com.mongodb.DBCollection;
import io.harness.cache.Distributable;
import io.harness.cache.DistributedStore;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import software.wings.beans.CacheEntity;

public class MongoStore implements DistributedStore {
  private Datastore datastore;
  private DBCollection collection;

  MongoStore(Datastore datastore) {
    this.datastore = datastore;
    collection = datastore.getCollection(CacheEntity.class);
  }

  @Override
  public <T extends Distributable> T get(long contextHash, long algorithmId, long structureHash, String key) {
    final Query<CacheEntity> query = datastore.getQueryFactory()
                                         .createQuery(datastore, collection, CacheEntity.class)
                                         .filter(CacheEntity.CONTEXT_HASH_KEY, contextHash)
                                         .filter(CacheEntity.ALGORITHM_ID_KEY, algorithmId)
                                         .filter(CacheEntity.STRUCTURE_HASH_KEY, structureHash)
                                         .filter(CacheEntity.KEY_KEY, key);
    final CacheEntity cacheEntity = query.get();

    if (cacheEntity == null) {
      return null;
    }

    return (T) cacheEntity.getEntity();
  }

  @Override
  public <T extends Distributable> void upsert(T entity) {
    final CacheEntity cacheEntity = CacheEntity.builder()
                                        .contextHash(entity.contextHash())
                                        .algorithmId(entity.algorithmId())
                                        .structureHash(entity.structureHash())
                                        .key(entity.key())
                                        .entity(entity)
                                        .build();
    datastore.save("cache", cacheEntity);
  }
}
