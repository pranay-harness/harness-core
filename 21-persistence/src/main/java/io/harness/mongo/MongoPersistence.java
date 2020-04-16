package io.harness.mongo;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.mongo.MongoUtils.setUnsetOnInsert;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.DBCollection;
import com.mongodb.DuplicateKeyException;
import com.mongodb.WriteResult;
import io.harness.mongo.SampleEntity.SampleEntityKeys;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.HPersistence;
import io.harness.persistence.HQuery;
import io.harness.persistence.HQuery.QueryChecks;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.Store;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UserProvider;
import io.harness.persistence.UuidAccess;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.InsertOptions;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateOpsImpl;
import org.mongodb.morphia.query.UpdateResults;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Singleton
@Slf4j
public class MongoPersistence implements HPersistence {
  @Value
  @Builder
  private static class Info {
    private String uri;
    // private Set<Class> classes;
  }

  @Inject Morphia morphia;

  private Map<String, Info> storeInfo = new HashMap<>();
  private Map<Class, Store> classStores = new HashMap<>();
  private Map<String, AdvancedDatastore> datastoreMap;
  UserProvider userProvider;

  @Inject
  public MongoPersistence(@Named("primaryDatastore") AdvancedDatastore primaryDatastore) {
    datastoreMap = new HashMap<>();
    datastoreMap.put(DEFAULT_STORE.getName(), primaryDatastore);
  }

  @Override
  public Duration healthExpectedResponseTimeout() {
    return ofSeconds(5);
  }

  @Override
  public Duration healthValidFor() {
    return ofSeconds(15);
  }

  @Override
  public void isHealthy() {
    List<AdvancedDatastore> datastores = datastoreMap.values().stream().distinct().collect(toList());
    for (AdvancedDatastore datastore : datastores) {
      datastore.getDB().getStats();
    }
  }

  @Override
  public void register(Store store, String uri) {
    storeInfo.put(store.getName(), Info.builder().uri(uri).build());
  }

  @Override
  public void registerUserProvider(UserProvider userProvider) {
    this.userProvider = userProvider;
  }

  @Override
  public AdvancedDatastore getDatastore(Store store) {
    return datastoreMap.computeIfAbsent(store.getName(), key -> {
      Info info = storeInfo.get(store.getName());
      if (info == null || isEmpty(info.getUri())) {
        return getDatastore(DEFAULT_STORE);
      }
      return MongoModule.createDatastore(morphia, info.getUri());
    });
  }

  @Override
  public Map<Class, Store> getClassStores() {
    return classStores;
  }

  @Override
  public DBCollection getCollection(Store store, String collectionName) {
    return getDatastore(store).getDB().getCollection(collectionName);
  }

  @Override
  public DBCollection getCollection(Class cls) {
    AdvancedDatastore datastore = getDatastore(cls);
    return datastore.getDB().getCollection(datastore.getCollection(cls).getName());
  }

  @Override
  public void ensureIndex(Class cls) {
    AdvancedDatastore datastore = getDatastore(cls);
    Morphia locMorphia = new Morphia();
    locMorphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());

    Set<Class> classSet = new HashSet<>();
    classSet.add(cls);
    locMorphia.map(classSet);

    IndexManager.ensureIndexes(datastore, locMorphia);
  }

  @Override
  public void close() {
    Set<AdvancedDatastore> datastores = new HashSet<>();
    datastores.addAll(datastoreMap.values());

    for (AdvancedDatastore datastore : datastores) {
      datastore.getMongo().close();
    }
  }

  @Override
  public <T extends PersistentEntity> Query<T> createQuery(Class<T> cls) {
    return getDatastore(cls).createQuery(cls);
  }

  @Override
  public <T extends PersistentEntity> Query<T> createQuery(Class<T> cls, Set<QueryChecks> queryChecks) {
    Query<T> query = createQuery(cls);
    ((HQuery) query).setQueryChecks(queryChecks);
    return query;
  }

  @Override
  public <T extends PersistentEntity> UpdateOperations<T> createUpdateOperations(Class<T> cls) {
    return getDatastore(cls).createUpdateOperations(cls);
  }

  private <T extends PersistentEntity> void onEntityUpdate(T entity, long currentTime) {
    if (entity instanceof UpdatedByAware) {
      UpdatedByAware updatedFromAware = (UpdatedByAware) entity;
      updatedFromAware.setLastUpdatedBy(userProvider.activeUser());
    }
    if (entity instanceof UpdatedAtAware) {
      UpdatedAtAware updatedAtAware = (UpdatedAtAware) entity;
      updatedAtAware.setLastUpdatedAt(currentTime);
    }
  }

  private <T extends PersistentEntity> void onSave(T entity) {
    if (entity instanceof UuidAware) {
      UuidAware uuidAware = (UuidAware) entity;
      if (uuidAware.getUuid() == null) {
        uuidAware.setUuid(generateUuid());
      }
    }

    long currentTime = currentTimeMillis();
    if (entity instanceof CreatedByAware) {
      CreatedByAware createdByAware = (CreatedByAware) entity;
      if (createdByAware.getCreatedBy() == null) {
        createdByAware.setCreatedBy(userProvider.activeUser());
      }
    }
    if (entity instanceof CreatedAtAware) {
      CreatedAtAware createdAtAware = (CreatedAtAware) entity;
      if (createdAtAware.getCreatedAt() == 0) {
        createdAtAware.setCreatedAt(currentTime);
      }
    }

    onEntityUpdate(entity, currentTime);
  }

  @Override
  public <T extends PersistentEntity> String save(T entity) {
    onSave(entity);
    AdvancedDatastore datastore = getDatastore(entity);
    return HPersistence.retry(() -> datastore.save(entity).getId().toString());
  }

  @Override
  public <T extends PersistentEntity> List<String> save(List<T> ts) {
    ts.removeIf(Objects::isNull);
    List<String> ids = new ArrayList<>();
    for (T entity : ts) {
      ids.add(save(entity));
    }
    return ids;
  }

  @Override
  public <T extends PersistentEntity> void saveIgnoringDuplicateKeys(List<T> ts) {
    for (Iterator<T> iterator = ts.iterator(); iterator.hasNext();) {
      T entity = iterator.next();
      if (entity == null) {
        iterator.remove();
        continue;
      }
      onSave(entity);
    }

    if (isEmpty(ts)) {
      return;
    }

    AdvancedDatastore datastore = getDatastore(ts.get(0));

    InsertOptions insertOptions = new InsertOptions();
    insertOptions.continueOnError(true);
    try {
      HPersistence.retry(() -> datastore.insert(ts, insertOptions));
    } catch (DuplicateKeyException ignore) {
      // ignore
    }
  }

  @Override
  public <T extends PersistentEntity> String insert(T entity) {
    onSave(entity);
    AdvancedDatastore datastore = getDatastore(entity);
    return HPersistence.retry(() -> datastore.insert(entity).getId().toString());
  }

  @Override
  public <T extends PersistentEntity> String insertIgnoringDuplicateKeys(T entity) {
    onSave(entity);
    AdvancedDatastore datastore = getDatastore(entity);
    try {
      return HPersistence.retry(() -> datastore.insert(entity).getId().toString());
    } catch (DuplicateKeyException ignore) {
      // ignore
    }
    if (entity instanceof UuidAccess) {
      return ((UuidAccess) entity).getUuid();
    }

    return null;
  }

  @Override
  public <T extends PersistentEntity> T get(Class<T> cls, String id) {
    return createQuery(cls).filter(ID_KEY, id).get();
  }

  @Override
  public <T extends PersistentEntity> boolean delete(Class<T> cls, String uuid) {
    AdvancedDatastore datastore = getDatastore(cls);
    return HPersistence.retry(() -> {
      WriteResult result = datastore.delete(cls, uuid);
      return !(result == null || result.getN() == 0);
    });
  }

  @Override
  public <T extends PersistentEntity> boolean delete(Query<T> query) {
    AdvancedDatastore datastore = getDatastore(query.getEntityClass());
    return HPersistence.retry(() -> {
      WriteResult result = datastore.delete(query);
      return !(result == null || result.getN() == 0);
    });
  }

  @Override
  public <T extends PersistentEntity> boolean delete(T entity) {
    AdvancedDatastore datastore = getDatastore(entity);
    return HPersistence.retry(() -> {
      WriteResult result = datastore.delete(entity);
      return !(result == null || result.getN() == 0);
    });
  }

  @Override
  public <T extends PersistentEntity> T upsert(
      Query<T> query, UpdateOperations<T> updateOperations, FindAndModifyOptions options) {
    // TODO: add encryption handling; right now no encrypted classes use upsert
    // When necessary, we can fix this by adding Class<T> cls to the args and then similar to updateField

    long currentTime = currentTimeMillis();

    if (CreatedByAware.class.isAssignableFrom(query.getEntityClass())) {
      setUnsetOnInsert(updateOperations, SampleEntityKeys.createdBy, userProvider.activeUser());
    }

    if (CreatedAtAware.class.isAssignableFrom(query.getEntityClass())) {
      updateOperations.setOnInsert(SampleEntityKeys.createdAt, currentTime);
    }

    onUpdate(query, updateOperations, currentTime);
    // TODO: this ignores the read preferences in the query
    AdvancedDatastore datastore = getDatastore(query.getEntityClass());
    return HPersistence.retry(() -> datastore.findAndModify(query, updateOperations, options));
  }

  private <T extends PersistentEntity> void onUpdate(
      Query<T> query, UpdateOperations<T> updateOperations, long currentTime) {
    if (UpdatedByAware.class.isAssignableFrom(query.getEntityClass())) {
      MongoUtils.setUnset(updateOperations, SampleEntityKeys.lastUpdatedBy, userProvider.activeUser());
    }

    if (UpdatedAtAware.class.isAssignableFrom(query.getEntityClass())) {
      updateOperations.set(SampleEntityKeys.lastUpdatedAt, currentTime);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Update {} with {}", query.getEntityClass().getName(),
          ((UpdateOpsImpl) updateOperations).getOps().toString());
    }
  }

  @Override
  public <T extends PersistentEntity> UpdateResults update(T entity, UpdateOperations<T> ops) {
    // TODO: add encryption handling; right now no encrypted classes use update
    // When necessary, we can fix this by adding Class<T> cls to the args and then similar to updateField
    onEntityUpdate(entity, currentTimeMillis());
    AdvancedDatastore datastore = getDatastore(entity);
    return HPersistence.retry(() -> datastore.update(entity, ops));
  }

  @Override
  public <T extends PersistentEntity> UpdateResults update(Query<T> updateQuery, UpdateOperations<T> updateOperations) {
    // TODO: add encryption handling; right now no encrypted classes use update
    // When necessary, we can fix this by adding Class<T> cls to the args and then similar to updateField
    onUpdate(updateQuery, updateOperations, currentTimeMillis());
    AdvancedDatastore datastore = getDatastore(updateQuery.getEntityClass());
    return HPersistence.retry(() -> datastore.update(updateQuery, updateOperations));
  }

  @Override
  public <T extends PersistentEntity> T findAndModify(
      Query<T> query, UpdateOperations<T> updateOperations, FindAndModifyOptions findAndModifyOptions) {
    onUpdate(query, updateOperations, currentTimeMillis());
    AdvancedDatastore datastore = getDatastore(query.getEntityClass());
    return HPersistence.retry(() -> datastore.findAndModify(query, updateOperations, findAndModifyOptions));
  }

  @Override
  public <T extends PersistentEntity> T findAndModifySystemData(
      Query<T> query, UpdateOperations<T> updateOperations, FindAndModifyOptions findAndModifyOptions) {
    AdvancedDatastore datastore = getDatastore(query.getEntityClass());
    return HPersistence.retry(() -> datastore.findAndModify(query, updateOperations, findAndModifyOptions));
  }

  @Override
  public <T extends PersistentEntity> String merge(T entity) {
    onEntityUpdate(entity, currentTimeMillis());
    AdvancedDatastore datastore = getDatastore(entity);
    return HPersistence.retry(() -> datastore.merge(entity).getId().toString());
  }
}
