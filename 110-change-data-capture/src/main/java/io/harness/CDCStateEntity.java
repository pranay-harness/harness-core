/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Entity(value = "cdcStateEntity", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "cdcStateEntityKeys")
@StoreIn("change-data-capture")
@OwnedBy(HarnessTeam.CE)
public class CDCStateEntity implements PersistentEntity {
  @Id private String sourceEntityClass;
  private String lastSyncedToken;
}
