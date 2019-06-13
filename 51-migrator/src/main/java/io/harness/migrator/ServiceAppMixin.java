package io.harness.migrator;

import static io.harness.beans.database.MigrationJobInstance.Status.BASELINE;
import static io.harness.beans.database.MigrationJobInstance.Status.PENDING;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.database.MigrationJobInstance;
import io.harness.beans.migration.MigrationJob;
import io.harness.beans.migration.MigrationList;
import io.harness.beans.migration.MongoCollectionMigrationChannel;
import io.harness.persistence.HPersistence;
import io.harness.persistence.ReadPref;
import io.harness.persistence.Store;
import io.harness.threading.Poller;

import java.time.Duration;
import java.util.Set;

@Singleton
public class ServiceAppMixin {
  @Inject private HPersistence persistence;

  private void upsertMigrationJobInstance(MigrationJob job) {
    final String inserted = persistence.insert(
        MigrationJobInstance.builder().id(job.getId()).metadata(job.getMetadata()).status(PENDING).build());

    // It was there from before
    // if (inserted == null) {
    // TODO: remove the allowances that are no longer acceptable
    //}
  }

  public boolean initIfFirstTime() {
    final Set<String> collectionNames =
        persistence.getDatastore(MigrationJobInstance.class, ReadPref.NORMAL).getDB().getCollectionNames();

    if (collectionNames.contains("migrationJobInstances")) {
      return false;
    }

    for (MigrationJob job : MigrationList.jobs.values()) {
      persistence.save(
          MigrationJobInstance.builder().id(job.getId()).metadata(job.getMetadata()).status(BASELINE).build());
    }

    return true;
  }

  public MigrationJob updateStoreMigrationJobInstances(Store store) {
    if (initIfFirstTime()) {
      return null;
    }
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
