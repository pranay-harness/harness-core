package software.wings.common.cache;

import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cache.Distributable;
import io.harness.cache.DistributedStore;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.CacheEntity;
import software.wings.dl.WingsPersistence;
import software.wings.utils.KryoUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;

@Singleton
public class MongoStore implements DistributedStore {
  private static final Logger logger = LoggerFactory.getLogger(MongoStore.class);
  private static String collectionName = "cache";

  @Inject WingsPersistence wingsPersistence;

  String canonicalKey(long algorithmId, long structureHash, String key) {
    return format("%s/%d/%d", key, algorithmId, structureHash);
  }

  @Override
  public <T extends Distributable> T get(long contextHash, long algorithmId, long structureHash, String key) {
    try {
      final Datastore datastore = wingsPersistence.getDatastore();
      final QueryFactory factory = datastore.getQueryFactory();

      final CacheEntity cacheEntity =
          factory.createQuery(datastore, wingsPersistence.getCollection(collectionName), CacheEntity.class)
              .filter(CacheEntity.CONTEXT_HASH_KEY, contextHash)
              .filter(CacheEntity.CANONICAL_KEY_KEY, canonicalKey(algorithmId, structureHash, key))
              .get();

      if (cacheEntity == null) {
        return null;
      }

      return (T) KryoUtils.asObject(cacheEntity.getEntity());
    } catch (RuntimeException ex) {
      logger.error("Failed to obtain from cache", ex);
    }
    return null;
  }

  @Override
  public <T extends Distributable> void upsert(T entity, Duration ttl) {
    try {
      final CacheEntity cacheEntity =
          CacheEntity.builder()
              .contextHash(entity.contextHash())
              .canonicalKey(canonicalKey(entity.algorithmId(), entity.structureHash(), entity.key()))
              .entity(KryoUtils.asBytes(entity))
              .validUntil(Date.from(OffsetDateTime.now().plus(ttl).toInstant()))
              .build();
      wingsPersistence.getDatastore().save(collectionName, cacheEntity);
    } catch (RuntimeException ex) {
      logger.error("Failed to update cache", ex);
    }
  }
}
