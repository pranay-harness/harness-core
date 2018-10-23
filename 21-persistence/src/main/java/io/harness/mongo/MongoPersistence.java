package io.harness.mongo;

import static io.harness.persistence.ReadPref.CRITICAL;
import static io.harness.persistence.ReadPref.NORMAL;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.DBCollection;
import io.harness.persistence.HPersistence;
import io.harness.persistence.ReadPref;
import io.harness.persistence.Store;
import org.mongodb.morphia.AdvancedDatastore;

import java.util.Map;

@Singleton
public class MongoPersistence implements HPersistence {
  private Map<String, AdvancedDatastore> datastoreMap;

  @Inject
  public MongoPersistence(@Named("primaryDatastore") AdvancedDatastore primaryDatastore,
      @Named("secondaryDatastore") AdvancedDatastore secondaryDatastore) {
    this.datastoreMap =
        ImmutableMap.of(key(DEFAULT_STORE, NORMAL), secondaryDatastore, key(DEFAULT_STORE, CRITICAL), primaryDatastore);
  }

  private String key(Store store, ReadPref readPref) {
    return store.getName() + (readPref == null ? NORMAL.name() : readPref.name());
  }

  @Override
  public AdvancedDatastore getDatastore(Store store, ReadPref readPref) {
    return datastoreMap.get(key(store, readPref));
  }

  @Override
  public DBCollection getCollection(Store store, ReadPref readPref, String collectionName) {
    return getDatastore(store, readPref).getDB().getCollection(collectionName);
  }
}
