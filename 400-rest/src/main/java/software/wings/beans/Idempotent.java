/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.distribution.idempotence.IdempotentResult;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.PersistentEntity;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@FieldNameConstants(innerTypeName = "IdempotentKeys")
@Entity(value = "idempotent_locks", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class Idempotent implements PersistentEntity {
  @Id private String uuid;

  public static final String TENTATIVE = "tentative";
  public static final String SUCCEEDED = "succeeded";

  private String state;
  private List<IdempotentResult> result;

  @Default @FdTtlIndex private Date validUntil = Date.from(OffsetDateTime.now().plusDays(3).toInstant());
}
