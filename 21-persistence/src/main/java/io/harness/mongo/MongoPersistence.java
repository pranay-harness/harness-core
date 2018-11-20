package io.harness.mongo;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.ReadPref.CRITICAL;
import static io.harness.persistence.ReadPref.NORMAL;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.DBCollection;
import com.mongodb.DuplicateKeyException;
import com.mongodb.WriteResult;
import io.harness.persistence.HPersistence;
import io.harness.persistence.HQuery;
import io.harness.persistence.HQuery.QueryChecks;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.ReadPref;
import io.harness.persistence.Store;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.InsertOptions;
import org.mongodb.morphia.query.Query;

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
  public <T extends PersistentEntity> String save(T object) {
    return getDatastore(object, ReadPref.NORMAL).save(object).getId().toString();
  }

  @Override
  public <T extends PersistentEntity> List<String> save(List<T> ts) {
    ts.removeIf(Objects::isNull);
    List<String> ids = new ArrayList<>();
    for (T t : ts) {
      ids.add(save(t));
    }
    return ids;
  }

  @Override
  public <T extends PersistentEntity> void saveIgnoringDuplicateKeys(List<T> ts) {
    for (Iterator<T> iterator = ts.iterator(); iterator.hasNext();) {
      T t = iterator.next();
      if (t == null) {
        iterator.remove();
        continue;
      }
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
}
