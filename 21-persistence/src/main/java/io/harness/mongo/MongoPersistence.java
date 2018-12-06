package io.harness.mongo;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.ReadPref.CRITICAL;
import static io.harness.persistence.ReadPref.NORMAL;
import static java.lang.System.currentTimeMillis;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.DBCollection;
import com.mongodb.DuplicateKeyException;
import com.mongodb.WriteResult;
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
import org.mongodb.morphia.InsertOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

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
  @Builder
  @Value
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
    return getDatastore(cls, ReadPref.NORMAL).createUpdateOperations(cls);
  }

  private <T extends PersistentEntity> void onUpdate(T entity, long currentTime) {
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

    onUpdate((T) entity, currentTime);
  }

  @Override
  public <T extends PersistentEntity> String save(T entity) {
    onSave(entity);
    return getDatastore(entity, ReadPref.NORMAL).save(entity).getId().toString();
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

    final AdvancedDatastore datastore = getDatastore(ts.get(0), ReadPref.NORMAL);

    InsertOptions insertOptions = new InsertOptions();
    insertOptions.continueOnError(true);
    try {
      datastore.insert(ts, insertOptions);
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
    return createQuery(cls, readPref).filter(ID_KEY, id).get();
  }

  @Override
  public <T extends PersistentEntity> boolean delete(Class<T> cls, String uuid) {
    final AdvancedDatastore datastore = getDatastore(cls, ReadPref.NORMAL);
    WriteResult result = datastore.delete(cls, uuid);
    return !(result == null || result.getN() == 0);
  }

  @Override
  public <T extends PersistentEntity> boolean delete(Query<T> query) {
    WriteResult result = getDatastore(query.getEntityClass(), ReadPref.NORMAL).delete(query);
    return !(result == null || result.getN() == 0);
  }

  @Override
  public <T extends PersistentEntity> boolean delete(T entity) {
    WriteResult result = getDatastore(entity, ReadPref.NORMAL).delete(entity);
    return !(result == null || result.getN() == 0);
  }

  @Override
  public <T extends PersistentEntity> String merge(T entity) {
    onUpdate((T) entity, currentTimeMillis());
    return getDatastore(entity, ReadPref.NORMAL).merge(entity).getId().toString();
  }
}
