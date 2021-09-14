/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import io.harness.annotation.StoreIn;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import java.time.OffsetDateTime;
import java.util.Date;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@Entity(value = "!!!custom_delegateAsyncTaskResponses", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "DelegateAsyncTaskResponseKeys")
@StoreIn(DbAliases.ALL)
public class DelegateAsyncTaskResponse implements PersistentEntity {
  @Id @org.springframework.data.annotation.Id private String uuid;
  private byte[] responseData;
  @FdIndex private long processAfter;
  private Long holdUntil;

  @FdTtlIndex @Builder.Default private Date validUntil = Date.from(OffsetDateTime.now().plusHours(24).toInstant());
}
