package io.harness.migrator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.database.MigrationJobInstance;
import io.harness.beans.migration.MigrationJob;
import io.harness.beans.migration.MigrationList;
import io.harness.beans.migration.MongoCollectionMigrationChannel;
import io.harness.persistence.HPersistence;
import io.harness.persistence.Store;

@Singleton
public class ServiceAppMixin {
  @Inject private HPersistence persistence;

  private void upsertMigrationJobInstance(MigrationJob job) {
    final String inserted =
        persistence.insert(MigrationJobInstance.builder().id(job.getId()).metadata(job.getMetadata()).build());

    // It was there from before
    // if (inserted == null) {
    // TODO: remove the allowances that are no longer acceptable
    //}
  }

  private void updateStoreMigrationJobInstances(Store store) {
    for (MigrationJob job : MigrationList.jobs.values()) {
      final boolean anyMatch = job.getMetadata()
                                   .getChannels()
                                   .stream()
                                   .filter(channel -> channel instanceof MongoCollectionMigrationChannel)
                                   .map(channel -> (MongoCollectionMigrationChannel) channel)
                                   .anyMatch(channel -> channel.getStore().getName().equals(store.getName()));

      upsertMigrationJobInstance(job);
    }
  }

  public void waitForMongoSchema(Store store) {
    updateStoreMigrationJobInstances(store);
  }
}
