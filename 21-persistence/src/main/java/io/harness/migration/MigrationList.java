package io.harness.migration;

import com.google.common.collect.ImmutableMap;

import io.harness.migration.MigrationJob.Metadata;
import io.harness.persistence.HPersistence;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class MigrationList {
  public static final Map<Integer, MigrationJob> jobs =
      ImmutableMap.<Integer, MigrationJob>builder()
          .put(1,
              MigrationJob.builder()
                  .id("test")
                  .sha("1e547fa017126116601bb7f6acf5a83be4e8d1b6")
                  .metadata(Metadata.builder()
                                .channel(MongoCollectionMigrationChannel.builder()
                                             .store(HPersistence.DEFAULT_STORE)
                                             .collection("foo")
                                             .build())
                                .build())
                  .build())
          .build();
}
