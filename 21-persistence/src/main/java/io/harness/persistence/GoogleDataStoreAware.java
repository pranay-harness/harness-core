package io.harness.persistence;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;

public interface GoogleDataStoreAware {
  Entity convertToCloudStorageEntity(Datastore datastore);

  GoogleDataStoreAware readFromCloudStorageEntity(Entity entity);
}