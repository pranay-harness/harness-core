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

  public static void setUpdateOperations(
      UpdateOperations<ActivitySource> updateOperations, CD10ActivitySourceDTO activitySourceDTO) {
    updateOperations.set(CD10ActivitySourceKeys.serviceMappings, activitySourceDTO.getServiceMappings());
    updateOperations.set(CD10ActivitySourceKeys.envMappings, activitySourceDTO.getEnvMappings());
  }
}
