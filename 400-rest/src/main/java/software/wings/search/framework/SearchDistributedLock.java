/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.validation.Update;

import java.util.Date;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * The search lock bean based on
 * Mongo TTL indexes
 *
 * @author utkarsh
 */
@OwnedBy(PL)
@Value
@Builder
@Entity(value = "searchDistributedLocks", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "SearchDistributedLockKeys")
public class SearchDistributedLock implements PersistentEntity, CreatedAtAccess {
  @Id @NotNull(groups = {Update.class}) private String name;
  @FdIndex @NotNull private String uuid;
  @FdTtlIndex(70) @NotNull private Date heartbeat;
  @NotNull private long createdAt;
}
