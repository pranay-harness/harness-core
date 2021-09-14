package io.harness.feature.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.feature.constants.RestrictionType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(HarnessTeam.GTM)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureDetailsDTO {
  private FeatureRestriction name;
  private String description;
  private String moduleType;
  private boolean allowed;
  private RestrictionType restrictionType;
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "restrictionType", include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      visible = true)
  private RestrictionDTO restriction;
}
