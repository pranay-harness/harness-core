/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migration.entities;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityName;
import io.harness.migration.beans.MigrationType;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

@Data
@Builder
@FieldNameConstants(innerTypeName = "NGSchemaKeys")
@OwnedBy(DX)
@AllArgsConstructor
public class NGSchema implements PersistentEntity {
  public static final String NG_SCHEMA_ID = "ngschema";
  @JsonIgnore @Id String id;
  @NotEmpty @CreatedDate Long createdAt;
  @LastModifiedDate Long lastUpdatedAt;
  @NotEmpty @EntityName String name;
  Map<MigrationType, Integer> migrationDetails;
}
