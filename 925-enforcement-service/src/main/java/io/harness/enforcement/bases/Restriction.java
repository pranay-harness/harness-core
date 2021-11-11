package io.harness.enforcement.bases;

import io.harness.enforcement.constants.RestrictionType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "restrictionType",
    visible = true)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = AvailabilityRestriction.class, name = "AVAILABILITY")
      , @JsonSubTypes.Type(value = StaticLimitRestriction.class, name = "STATIC_LIMIT"),
          @JsonSubTypes.Type(value = RateLimitRestriction.class, name = "RATE_LIMIT"),
          @JsonSubTypes.Type(value = CustomRestriction.class, name = "CUSTOM"),
          @JsonSubTypes.Type(value = DurationRestriction.class, name = "DURATION"),
          @JsonSubTypes.Type(value = LicenseLimitRestriction.class, name = "LICENSE_LIMIT"),
    })
public abstract class Restriction {
  protected RestrictionType restrictionType;

  public RestrictionType getRestrictionType() {
    return restrictionType;
  }
}
