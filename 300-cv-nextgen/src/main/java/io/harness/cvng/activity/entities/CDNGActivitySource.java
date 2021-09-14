/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.activity.entities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.beans.activity.ActivitySourceType;
import io.harness.cvng.beans.activity.cdng.CDNGActivitySourceDTO;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@Data
@NoArgsConstructor
@SuperBuilder
@FieldNameConstants(innerTypeName = "CDNGActivitySourceKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CV)
public class CDNGActivitySource extends ActivitySource {
  public static final String CDNG_ACTIVITY_SOURCE_IDENTIFIER = "cd_nextgen_activity_source";
  public static final String CDNG_ACTIVITY_SOURCE_NAME = "CD Nextgen Activity Source";
  @Override
  public ActivitySourceDTO toDTO() {
    return fillCommon(CDNGActivitySourceDTO.builder()).build();
  }

  @Override
  protected void validateParams() {
    // no-op
  }

  public static CDNGActivitySource getDefaultObject(String accountId, String orgIdentifier, String projectIdentifier) {
    return CDNGActivitySource.builder()
        .accountId(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .identifier(CDNG_ACTIVITY_SOURCE_IDENTIFIER)
        .name(CDNG_ACTIVITY_SOURCE_NAME)
        .type(ActivitySourceType.CDNG)
        .build();
  }

  public static class CDNGActivitySourceUpdatableEntity<T extends CDNGActivitySource, D extends CDNGActivitySourceDTO>
      extends ActivitySourceUpdatableEntity<T, D> {
    @Override
    public void setUpdateOperations(UpdateOperations<T> updateOperations, D dto) {
      throw new IllegalStateException("CDNG activity can not be updated using the API.");
    }
  }
}
