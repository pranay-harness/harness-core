/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@AllArgsConstructor
@org.mongodb.morphia.annotations.Entity(value = "timeSeriesKeyTransactions", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "TimeSeriesKeyTransactionsKeys")
public class TimeSeriesKeyTransactions implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware,
                                                  UpdatedAtAware, UpdatedByAware, AccountAccess {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  private EmbeddedUser createdBy;
  private EmbeddedUser lastUpdatedBy;

  @FdIndex private String cvConfigId;
  @FdIndex private String serviceId;
  @FdIndex private String accountId;
  private Set<String> keyTransactions;
}
