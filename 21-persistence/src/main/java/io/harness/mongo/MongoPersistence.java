package io.harness.mongo;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.ReadPref.CRITICAL;
import static io.harness.persistence.ReadPref.NORMAL;
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
import io.harness.persistence.ReadPref;
import io.harness.persistence.Store;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UserProvider;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.InsertOptions;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
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
public class MongoPersistence implements HPersistence {
  @Value
  @Builder
  private static class Info {
    private String uri;
    private Set<Class> classes;
  }

  private Map<String, Info> storeInfo = new HashMap<>();
  private Map<Class, Store> classStores = new HashMap<>();
  private Map<String, AdvancedDatastore> datastoreMap;
  UserProvider userProvider;

  @Inject
  public MongoPersistence(@Named("primaryDatastore") AdvancedDatastore primaryDatastore,
      @Named("secondaryDatastore") AdvancedDatastore secondaryDatastore) {
    datastoreMap = new HashMap<>();
    datastoreMap.put(key(DEFAULT_STORE, NORMAL), primaryDatastore);
    datastoreMap.put(key(DEFAULT_STORE, CRITICAL), secondaryDatastore);
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
  public void isHealthy() throws Exception {
    final List<AdvancedDatastore> datastores = datastoreMap.values().stream().distinct().collect(toList());
    for (AdvancedDatastore datastore : datastores) {
      datastore.getDB().getStats();
    }
  }

  private String key(Store store, ReadPref readPref) {
    return store.getName() + (readPref == null ? NORMAL.name() : readPref.name());
  }

  @Override
  public void register(Store store, String uri, Set<Class> classes) {
    storeInfo.put(store.getName(), Info.builder().uri(uri).classes(classes).build());
  }

  @Override
  public void registerUserProvider(UserProvider userProvider) {
    this.userProvider = userProvider;
  }

  @Override
  public AdvancedDatastore getDatastore(Store store, ReadPref readPref) {
    return datastoreMap.computeIfAbsent(key(store, readPref), key -> {
      final Info info = storeInfo.get(store.getName());
      if (info == null || isEmpty(info.getUri())) {
        return getDatastore(DEFAULT_STORE, readPref);
      }
      return MongoModule.createDatastore(info.getUri(), info.getClasses(), readPref);
    });
  }

  @Override
  public Map<Class, Store> getClassStores() {
    return classStores;
  }

  @Override
  public DBCollection getCollection(Store store, ReadPref readPref, String collectionName) {
    return getDatastore(store, readPref).getDB().getCollection(collectionName);
  }

  @Override
  public DBCollection getCollection(Class cls, ReadPref readPref) {
    final AdvancedDatastore datastore = getDatastore(cls, readPref);
    return datastore.getDB().getCollection(datastore.getCollection(cls).getName());
  }

  @Override
  public void ensureIndex(Class cls) {
    AdvancedDatastore datastore = getDatastore(cls, NORMAL);
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());

    Set<Class> classSet = new HashSet<>();
    classSet.add(cls);
    morphia.map(classSet);

    IndexManagement.ensureIndex(datastore, morphia);
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
    return createQuery(cls, NORMAL);
  }

  @Override
  public <T extends PersistentEntity> Query<T> createQuery(Class<T> cls, ReadPref readPref) {
    return getDatastore(cls, readPref).createQuery(cls);
  }

  @Override
  public <T extends PersistentEntity> Query<T> createQuery(Class<T> cls, Set<QueryChecks> queryChecks) {
    Query<T> query = createQuery(cls, NORMAL);
    ((HQuery) query).setQueryChecks(queryChecks);
    return query;
  }

  @Override
  public <T extends PersistentEntity> Query<T> createQuery(
      Class<T> cls, ReadPref readPref, Set<QueryChecks> queryChecks) {
    Query<T> query = createQuery(cls, readPref);
    ((HQuery) query).setQueryChecks(queryChecks);
    return query;
  }

  @Override
  public <T extends PersistentEntity> UpdateOperations<T> createUpdateOperations(Class<T> cls) {
    return getDatastore(cls, NORMAL).createUpdateOperations(cls);
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

    final long currentTime = currentTimeMillis();
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
    final AdvancedDatastore datastore = getDatastore(entity, NORMAL);
    return HPersistence.retry(() -> { return datastore.save(entity).getId().toString(); });
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

    final AdvancedDatastore datastore = getDatastore(ts.get(0), NORMAL);

    InsertOptions insertOptions = new InsertOptions();
    insertOptions.continueOnError(true);
    try {
      HPersistence.retry(() -> { return datastore.insert(ts, insertOptions); });
    } catch (DuplicateKeyException ignore) {
      // ignore
    }
  }

  @Override
  public <T extends PersistentEntity> T get(Class<T> cls, String id) {
    return get(cls, id, NORMAL);
  }

  @Override
  public <T extends PersistentEntity> T get(Class<T> cls, String id, ReadPref readPref) {
    final Query<T> query = createQuery(cls, readPref).filter(ID_KEY, id);
    return query.get();
  }

  @Override
  public <T extends PersistentEntity> boolean delete(Class<T> cls, String uuid) {
    final AdvancedDatastore datastore = getDatastore(cls, NORMAL);
    return HPersistence.retry(() -> {
      WriteResult result = datastore.delete(cls, uuid);
      return !(result == null || result.getN() == 0);
    });
  }

  @Override
  public <T extends PersistentEntity> boolean delete(Query<T> query) {
    final AdvancedDatastore datastore = getDatastore(query.getEntityClass(), NORMAL);
    return HPersistence.retry(() -> {
      WriteResult result = datastore.delete(query);
      return !(result == null || result.getN() == 0);
    });
  }

  @Override
  public <T extends PersistentEntity> boolean delete(T entity) {
    final AdvancedDatastore datastore = getDatastore(entity, NORMAL);
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

    final long currentTime = currentTimeMillis();

    if (CreatedByAware.class.isAssignableFrom(query.getEntityClass())) {
      MongoUtils.setUnset(updateOperations, SampleEntityKeys.createdBy, userProvider.activeUser());
    }

    if (CreatedAtAware.class.isAssignableFrom(query.getEntityClass())) {
      updateOperations.setOnInsert(SampleEntityKeys.createdAt, currentTime);
    }

    onUpdate(query, updateOperations, currentTime);
    // TODO: this ignores the read preferences in the query
    final AdvancedDatastore datastore = getDatastore(query.getEntityClass(), NORMAL);
    return HPersistence.retry(() -> { return datastore.findAndModify(query, updateOperations, options); });
  }

  private <T extends PersistentEntity> void onUpdate(
      Query<T> query, UpdateOperations<T> updateOperations, long currentTime) {
    if (UpdatedByAware.class.isAssignableFrom(query.getEntityClass())) {
      MongoUtils.setUnset(updateOperations, SampleEntityKeys.lastUpdatedBy, userProvider.activeUser());
    }

    if (UpdatedAtAware.class.isAssignableFrom(query.getEntityClass())) {
      updateOperations.set(SampleEntityKeys.lastUpdatedAt, currentTime);
    }
  }

  @Override
  public <T extends PersistentEntity> UpdateResults update(T entity, UpdateOperations<T> ops) {
    // TODO: add encryption handling; right now no encrypted classes use update
    // When necessary, we can fix this by adding Class<T> cls to the args and then similar to updateField
    onEntityUpdate(entity, currentTimeMillis());
    final AdvancedDatastore datastore = getDatastore(entity, NORMAL);
    return HPersistence.retry(() -> { return datastore.update(entity, ops); });
  }

  @Override
  public <T extends PersistentEntity> UpdateResults update(Query<T> updateQuery, UpdateOperations<T> updateOperations) {
    // TODO: add encryption handling; right now no encrypted classes use update
    // When necessary, we can fix this by adding Class<T> cls to the args and then similar to updateField
    onUpdate(updateQuery, updateOperations, currentTimeMillis());
    final AdvancedDatastore datastore = getDatastore(updateQuery.getEntityClass(), NORMAL);
    return HPersistence.retry(() -> { return datastore.update(updateQuery, updateOperations); });
  }

  @Override
  public <T extends PersistentEntity> T findAndModify(
      Query<T> query, UpdateOperations<T> updateOperations, FindAndModifyOptions findAndModifyOptions) {
    onUpdate(query, updateOperations, currentTimeMillis());
    final AdvancedDatastore datastore = getDatastore(query.getEntityClass(), CRITICAL);
    return HPersistence.retry(() -> { return datastore.findAndModify(query, updateOperations, findAndModifyOptions); });
  }

  @Override
  public <T extends PersistentEntity> T findAndModifySystemData(
      Query<T> query, UpdateOperations<T> updateOperations, FindAndModifyOptions findAndModifyOptions) {
    final AdvancedDatastore datastore = getDatastore(query.getEntityClass(), CRITICAL);
    return HPersistence.retry(() -> { return datastore.findAndModify(query, updateOperations, findAndModifyOptions); });
  }

  @Override
  public <T extends PersistentEntity> String merge(T entity) {
    onEntityUpdate(entity, currentTimeMillis());
    final AdvancedDatastore datastore = getDatastore(entity, NORMAL);
    return HPersistence.retry(() -> { return datastore.merge(entity).getId().toString(); });
  }
}