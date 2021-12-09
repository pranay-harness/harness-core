package io.harness.enforcement.beans.details;

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
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "restrictionType", include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AvailabilityRestrictionDTO.class, name = "AVAILABILITY")
  , @JsonSubTypes.Type(value = StaticLimitRestrictionDTO.class, name = "STATIC_LIMIT"),
      @JsonSubTypes.Type(value = RateLimitRestrictionDTO.class, name = "RATE_LIMIT"),
      @JsonSubTypes.Type(value = CustomRestrictionDTO.class, name = "CUSTOM"),
      @JsonSubTypes.Type(value = DurationRestrictionDTO.class, name = "DURATION"),
      @JsonSubTypes.Type(value = LicenseRateLimitRestrictionDTO.class, name = "LICENSE_RATE_LIMIT"),
      @JsonSubTypes.Type(value = LicenseStaticLimitRestrictionDTO.class, name = "LICENSE_STATIC_LIMIT"),
})
@Schema(name = "Restriction", description = "This contains details of the Restriction in Harness")
public abstract class RestrictionDTO {}
