/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.views.entities;

import io.harness.annotation.StoreIn;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@StoreIn(DbAliases.CENG)
@FieldNameConstants(innerTypeName = "ViewCustomFieldKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "viewCustomField", noClassnameStored = true)
public final class ViewCustomField
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountId_viewId_name")
                 .unique(true)
                 .field(ViewCustomFieldKeys.accountId)
                 .field(ViewCustomFieldKeys.viewId)
                 .field(ViewCustomFieldKeys.name)
                 .build())
        .build();
  }
  @Id String uuid;
  String accountId;
  String viewId;
  String name;
  String description;
  String sqlFormula;
  String displayFormula;
  String userDefinedExpression;
  List<ViewField> viewFields;

  long createdAt;
  long lastUpdatedAt;
}
