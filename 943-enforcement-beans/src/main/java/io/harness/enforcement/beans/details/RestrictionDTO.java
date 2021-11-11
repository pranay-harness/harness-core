package io.harness.enforcement.beans.details;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AvailabilityRestrictionDTO.class, name = "AVAILABILITY")
  , @JsonSubTypes.Type(value = StaticLimitRestrictionDTO.class, name = "STATIC_LIMIT"),
      @JsonSubTypes.Type(value = RateLimitRestrictionDTO.class, name = "RATE_LIMIT"),
      @JsonSubTypes.Type(value = CustomRestrictionDTO.class, name = "CUSTOM"),
      @JsonSubTypes.Type(value = DurationRestrictionDTO.class, name = "DURATION"),
      @JsonSubTypes.Type(value = LicenseLimitRestrictionDTO.class, name = "LICENSE_LIMIT"),
})
public abstract class RestrictionDTO {}
