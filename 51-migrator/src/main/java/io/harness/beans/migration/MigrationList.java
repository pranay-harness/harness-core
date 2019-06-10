package io.harness.beans.migration;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.migration.MigrationJob.Metadata;
import io.harness.persistence.HPersistence;

import java.util.Map;

public class MigrationList {
  public static final Map<Integer, MigrationJob> jobs =
      ImmutableMap.<Integer, MigrationJob>builder()
          .put(1,
              MigrationJob.builder()
                  .id("test")
                  .metadata(Metadata.builder()
                                .channel(MongoCollectionMigrationChannel.builder()
                                             .store(HPersistence.DEFAULT_STORE)
                                             .collection("foo")
                                             .build())
                                .build())
                  .build())
          .build();
}
