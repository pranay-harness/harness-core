package io.harness.mongo;

import static io.harness.persistence.ReadPref.CRITICAL;
import static io.harness.persistence.ReadPref.NORMAL;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.DBCollection;
import io.harness.persistence.HPersistence;
import io.harness.persistence.HQuery;
import io.harness.persistence.HQuery.QueryChecks;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.ReadPref;
import io.harness.persistence.Store;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.query.Query;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class MongoPersistence implements HPersistence {
  private Map<String, String> storeUri = new HashMap<>();
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
  public void register(Store store, String uri) {
    storeUri.put(store.getName(), uri);
  }

  @Override
  public AdvancedDatastore getDatastore(Store store, ReadPref readPref) {
    return datastoreMap.computeIfAbsent(
        key(store, readPref), key -> MongoModule.createDatastore(storeUri.get(store.getName()), readPref));
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
  public <T extends PersistentEntity> Query<T> createQuery(Class<T> cls) {
    return createQuery(cls, NORMAL);
  }

  @Override
  public <T extends PersistentEntity> Query<T> createQuery(Class<T> cls, ReadPref readPref) {
    return getDatastore(DEFAULT_STORE, readPref).createQuery(cls);
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
}
