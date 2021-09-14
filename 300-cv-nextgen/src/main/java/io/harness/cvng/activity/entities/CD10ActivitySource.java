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
import io.harness.cvng.beans.activity.cd10.CD10ActivitySourceDTO;
import io.harness.cvng.beans.activity.cd10.CD10EnvMappingDTO;
import io.harness.cvng.beans.activity.cd10.CD10ServiceMappingDTO;

import java.util.Collections;
import java.util.Set;
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
@FieldNameConstants(innerTypeName = "CD10ActivitySourceKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CV)
public class CD10ActivitySource extends ActivitySource {
  public static final String HARNESS_CD_10_ACTIVITY_SOURCE_IDENTIFIER = "harness_cd10_activity_source";
  private Set<CD10EnvMappingDTO> envMappings;
  private Set<CD10ServiceMappingDTO> serviceMappings;

  public Set<CD10ServiceMappingDTO> getServiceMappings() {
    if (serviceMappings == null) {
      return Collections.emptySet();
    }
    return serviceMappings;
  }

  public Set<CD10EnvMappingDTO> getEnvMappings() {
    if (envMappings == null) {
      return Collections.emptySet();
    }
    return envMappings;
  }

  @Override
  public ActivitySourceDTO toDTO() {
    return fillCommon(CD10ActivitySourceDTO.builder())
        .uuid(this.getUuid())
        .envMappings(envMappings)
        .serviceMappings(serviceMappings)
        .build();
  }

  @Override
  protected void validateParams() {
    // no op
  }

  public static CD10ActivitySource fromDTO(
      String accountId, String orgIdentifier, String projectIdentifier, CD10ActivitySourceDTO activitySourceDTO) {
    return CD10ActivitySource.builder()
        .uuid(activitySourceDTO.getUuid())
        .accountId(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .uuid(activitySourceDTO.getUuid())
        .identifier(activitySourceDTO.getIdentifier())
        .name(activitySourceDTO.getName())
        .serviceMappings(activitySourceDTO.getServiceMappings())
        .envMappings(activitySourceDTO.getEnvMappings())
        .type(ActivitySourceType.HARNESS_CD10)
        .build();
  }

  public static class CD10ActivitySourceUpdatableEntity<T extends CD10ActivitySource, D extends CD10ActivitySourceDTO>
      extends ActivitySourceUpdatableEntity<T, D> {
    @Override
    public void setUpdateOperations(UpdateOperations<T> updateOperations, D dto) {
      setCommonOperations(updateOperations, dto);
      updateOperations.set(CD10ActivitySourceKeys.serviceMappings, dto.getServiceMappings())
          .set(CD10ActivitySourceKeys.envMappings, dto.getEnvMappings());
    }
  }
}
