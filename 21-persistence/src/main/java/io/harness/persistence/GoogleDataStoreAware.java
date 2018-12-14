package io.harness.persistence;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;

public interface GoogleDataStoreAware extends PersistentEntity {
  Entity convertToCloudStorageEntity(Datastore datastore);

  GoogleDataStoreAware readFromCloudStorageEntity(Entity entity);
}