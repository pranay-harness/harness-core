package io.harness.migrator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.database.MigrationJobInstance;
import io.harness.beans.migration.MigrationJob;
import io.harness.beans.migration.MigrationList;
import io.harness.beans.migration.MongoCollectionMigrationChannel;
import io.harness.persistence.HPersistence;
import io.harness.persistence.Store;
import io.harness.threading.Poller;

import java.time.Duration;

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

  public MigrationJob updateStoreMigrationJobInstances(Store store) {
    MigrationJob lastJob = null;
    for (MigrationJob job : MigrationList.jobs.values()) {
      final boolean anyMatch = job.getMetadata()
                                   .getChannels()
                                   .stream()
                                   .filter(channel -> channel instanceof MongoCollectionMigrationChannel)
                                   .map(channel -> (MongoCollectionMigrationChannel) channel)
                                   .anyMatch(channel -> channel.getStore().getName().equals(store.getName()));

      if (anyMatch) {
        lastJob = job;
        upsertMigrationJobInstance(job);
      }
    }

    return lastJob;
  }

  public void waitForMongoSchema(Store store) {
    final MigrationJob lastJob = updateStoreMigrationJobInstances(store);

    Poller.pollFor(Duration.ofMinutes(10), Duration.ofSeconds(10), () -> {
      final MigrationJobInstance migrationJobInstance = persistence.get(MigrationJobInstance.class, lastJob.getId());
      return MigrationJobInstance.Status.isFinalStatus(migrationJobInstance.getStatus());
    });
  }
}
