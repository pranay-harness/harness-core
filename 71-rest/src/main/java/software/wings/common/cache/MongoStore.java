package software.wings.common.cache;

import static com.mongodb.ErrorCategory.DUPLICATE_KEY;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoCommandException;
import io.harness.cache.Distributable;
import io.harness.cache.DistributedStore;
import io.harness.cache.Nominal;
import io.harness.cache.Ordinal;
import io.harness.persistence.ReadPref;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryFactory;
import org.mongodb.morphia.query.UpdateOperations;
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
  private static final int version = 1;

  private static final String collectionName = "cache";

  @Inject WingsPersistence wingsPersistence;

  String canonicalKey(long algorithmId, long structureHash, String key) {
    return format("%s/%d/%d/%d", key, version, algorithmId, structureHash);
  }

  @Override
  public <T extends Distributable> T get(long algorithmId, long structureHash, String key) {
    return get(null, algorithmId, structureHash, key);
  }

  @Override
  public <T extends Distributable> T get(long contextValue, long algorithmId, long structureHash, String key) {
    return get(Long.valueOf(contextValue), algorithmId, structureHash, key);
  }

  private <T extends Distributable> T get(Long contextValue, long algorithmId, long structureHash, String key) {
    try {
      final Datastore datastore = wingsPersistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL);
      final QueryFactory factory = datastore.getQueryFactory();

      final Query<CacheEntity> entityQuery =
          factory
              .createQuery(datastore, wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, collectionName),
                  CacheEntity.class)
              .filter(CacheEntity.CANONICAL_KEY_KEY, canonicalKey(algorithmId, structureHash, key));

      if (contextValue != null) {
        entityQuery.filter(CacheEntity.CONTEXT_VALUE_KEY, contextValue);
      }

      final CacheEntity cacheEntity = entityQuery.get();

      if (cacheEntity == null) {
        return null;
      }

      return (T) KryoUtils.asInflatedObject(cacheEntity.getEntity());
    } catch (RuntimeException ex) {
      logger.error("Failed to obtain from cache", ex);
    }
    return null;
  }

  @Override
  public <T extends Distributable> void upsert(T entity, Duration ttl) {
    final String canonicalKey = canonicalKey(entity.algorithmId(), entity.structureHash(), entity.key());
    Long contextValue = null;
    if (entity instanceof Nominal) {
      contextValue = ((Nominal) entity).contextHash();
    } else if (entity instanceof Ordinal) {
      contextValue = ((Ordinal) entity).contextOrder();
    }
    try {
      final Datastore datastore = wingsPersistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL);
      final UpdateOperations<CacheEntity> updateOperations = datastore.createUpdateOperations(CacheEntity.class);
      updateOperations.set(CacheEntity.CONTEXT_VALUE_KEY, contextValue);
      updateOperations.set(CacheEntity.CANONICAL_KEY_KEY, canonicalKey);
      updateOperations.set(CacheEntity.ENTITY_KEY, KryoUtils.asDeflatedBytes(entity));
      updateOperations.set(CacheEntity.VALID_UNTIL_KEY, Date.from(OffsetDateTime.now().plus(ttl).toInstant()));

      final Query<CacheEntity> query =
          datastore.createQuery(CacheEntity.class).filter(CacheEntity.CANONICAL_KEY_KEY, canonicalKey);
      if (entity instanceof Ordinal) {
        // For ordinal data lets make sure we are not downgrading the cache
        query.field(CacheEntity.CONTEXT_VALUE_KEY).lessThan(contextValue);
      }

      datastore.findAndModify(query, updateOperations, false, true);
    } catch (MongoCommandException exception) {
      if (ErrorCategory.fromErrorCode(exception.getErrorCode()) != DUPLICATE_KEY) {
        logger.error("Failed to update cache for key {}, hash {}", canonicalKey, contextValue, exception);
      } else {
        new Exception().addSuppressed(exception);
      }
    } catch (DuplicateKeyException ignore) {
      // Unfortunately mongo does not seem to support atomic upsert. It is atomic update and the unique index will
      // prevent second record being stored, but competing calls will occasionally throw duplicate exception
    } catch (RuntimeException ex) {
      logger.error("Failed to update cache for key {}, hash {}", canonicalKey, contextValue, ex);
    }
  }
}
