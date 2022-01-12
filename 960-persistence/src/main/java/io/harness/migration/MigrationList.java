/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migration;

import io.harness.migration.MigrationJob.Metadata;
import io.harness.persistence.HPersistence;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

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
