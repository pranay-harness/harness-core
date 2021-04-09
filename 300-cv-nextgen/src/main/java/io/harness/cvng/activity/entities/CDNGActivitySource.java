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
}
