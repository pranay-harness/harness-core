package io.harness.cache;

import static com.mongodb.ErrorCategory.DUPLICATE_KEY;
import static io.harness.persistence.HPersistence.upsertReturnNewOptions;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoCommandException;
import io.harness.cache.CacheEntity.CacheEntityKeys;
import io.harness.data.structure.EmptyPredicate;
import io.harness.govern.IgnoreThrowable;
import io.harness.persistence.HPersistence;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Singleton
@Slf4j
public class MongoStore implements DistributedStore {
  private static final int version = 1;

  @Inject HPersistence hPersistence;

  String canonicalKey(long algorithmId, long structureHash, String key, List<String> params) {
    if (EmptyPredicate.isEmpty(params)) {
      return format("%s/%d/%d/%d", key, version, algorithmId, structureHash);
    }
    return format("%s/%d/%d/%d%d", key, version, algorithmId, structureHash, Objects.hash(params.toArray()));
  }

  @Override
  public <T extends Distributable> T get(long algorithmId, long structureHash, String key, List<String> params) {
    return get(null, algorithmId, structureHash, key, params);
  }

  @Override
  public <T extends Distributable> T get(
      long contextValue, long algorithmId, long structureHash, String key, List<String> params) {
    return get(Long.valueOf(contextValue), algorithmId, structureHash, key, params);
  }

  private <T extends Distributable> T get(
      Long contextValue, long algorithmId, long structureHash, String key, List<String> params) {
    try {
      final Query<CacheEntity> entityQuery =
          hPersistence.createQuery(CacheEntity.class)
              .filter(CacheEntityKeys.canonicalKey, canonicalKey(algorithmId, structureHash, key, params));

      if (contextValue != null) {
        entityQuery.filter(CacheEntityKeys.contextValue, contextValue);
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
    final String canonicalKey =
        canonicalKey(entity.algorithmId(), entity.structureHash(), entity.key(), entity.parameters());
    Long contextValue =
        entity instanceof Nominal ? ((Nominal) entity).contextHash() : ((Ordinal) entity).contextOrder();

    try {
      final UpdateOperations<CacheEntity> updateOperations = hPersistence.createUpdateOperations(CacheEntity.class);
      updateOperations.set(CacheEntityKeys.contextValue, contextValue);
      updateOperations.set(CacheEntityKeys.canonicalKey, canonicalKey);
      updateOperations.set(CacheEntityKeys.entity, KryoUtils.asDeflatedBytes(entity));
      updateOperations.set(CacheEntityKeys.validUntil, Date.from(OffsetDateTime.now().plus(ttl).toInstant()));

      final Query<CacheEntity> query =
          hPersistence.createQuery(CacheEntity.class).filter(CacheEntityKeys.canonicalKey, canonicalKey);
      if (entity instanceof Ordinal) {
        // For ordinal data lets make sure we are not downgrading the cache
        query.field(CacheEntityKeys.contextValue).lessThan(contextValue);
      }

      hPersistence.upsert(query, updateOperations, upsertReturnNewOptions);
    } catch (MongoCommandException exception) {
      if (ErrorCategory.fromErrorCode(exception.getErrorCode()) != DUPLICATE_KEY) {
        logger.error("Failed to update cache for key {}, hash {}", canonicalKey, contextValue, exception);
      } else {
        new Exception().addSuppressed(exception);
      }
    } catch (DuplicateKeyException ignore) {
      // Unfortunately mongo does not seem to support atomic upsert. It is atomic update and the unique index will
      // prevent second record being stored, but competing calls will occasionally throw duplicate exception
      IgnoreThrowable.ignoredOnPurpose(ignore);
    } catch (RuntimeException ex) {
      logger.error("Failed to update cache for key {}, hash {}", canonicalKey, contextValue, ex);
    }
  }
}
