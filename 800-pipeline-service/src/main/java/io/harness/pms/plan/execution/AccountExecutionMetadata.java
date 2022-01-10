/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PIPELINE)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "AccountExecutionMetadataKeys")
@Entity(value = "accountExecutionMetadata", noClassnameStored = true)
@Document("accountExecutionMetadata")
@TypeAlias("accountExecutionMetadata")
@HarnessEntity(exportable = true)
@StoreIn(DbAliases.PMS)
public class AccountExecutionMetadata {
  @Wither @Id @org.mongodb.morphia.annotations.Id String uuid;
  String accountId;
  @Builder.Default Map<String, Long> moduleToExecutionCount = new HashMap<>();
  @NonFinal @Builder.Default @Setter Map<String, AccountExecutionInfo> moduleToExecutionInfoMap = new HashMap<>();

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder().name("accountId_idx").field(AccountExecutionMetadataKeys.accountId).build())
        .build();
  }
}
