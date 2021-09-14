/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.polling.bean;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "PollingDocumentKeys")
@Entity(value = "pollingDocuments", noClassnameStored = true)
@Document("pollingDocuments")
@HarnessEntity(exportable = true)
@StoreIn(DbAliases.NG_MANAGER)
@OwnedBy(HarnessTeam.CDC)
public class PollingDocument implements PersistentEntity, AccountAccess, UuidAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountId_organizationIdentifier_projectIdentifier_pollingType_pollingItem")
                 .field(PollingDocumentKeys.accountId)
                 .field(PollingDocumentKeys.orgIdentifier)
                 .field(PollingDocumentKeys.projectIdentifier)
                 .field(PollingDocumentKeys.pollingType)
                 .field(PollingDocumentKeys.pollingInfo)
                 .field(PollingDocumentKeys.signatures)
                 .build())
        .add(CompoundMongoIndex.builder().name("accountId").field(PollingDocumentKeys.accountId).build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id private String uuid;

  @NotNull private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  @NotNull private List<String> signatures;

  @JsonProperty("type") private PollingType pollingType;

  private PollingInfo pollingInfo;

  private PolledResponse polledResponse;

  private String perpetualTaskId;

  private int failedAttempts;

  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
}
