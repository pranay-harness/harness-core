package io.harness.enforcement.beans.metadata;

import io.harness.enforcement.constants.RestrictionType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "restrictionType", include = JsonTypeInfo.As.EXISTING_PROPERTY,
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AvailabilityRestrictionMetadataDTO.class, name = "AVAILABILITY")
  , @JsonSubTypes.Type(value = StaticLimitRestrictionMetadataDTO.class, name = "STATIC_LIMIT"),
      @JsonSubTypes.Type(value = RateLimitRestrictionMetadataDTO.class, name = "RATE_LIMIT"),
      @JsonSubTypes.Type(value = CustomRestrictionMetadataDTO.class, name = "CUSTOM"),
      @JsonSubTypes.Type(value = DurationRestrictionMetadataDTO.class, name = "DURATION"),
      @JsonSubTypes.Type(value = LicenseRateLimitRestrictionMetadataDTO.class, name = "LICENSE_RATE_LIMIT"),
      @JsonSubTypes.Type(value = LicenseStaticLimitRestrictionMetadataDTO.class, name = "LICENSE_STATIC_LIMIT"),
})
@Schema(name = "RestrictionMetadata", description = "This contains metadata of the Restriction in Harness")
public abstract class RestrictionMetadataDTO {
  private RestrictionType restrictionType;
}
