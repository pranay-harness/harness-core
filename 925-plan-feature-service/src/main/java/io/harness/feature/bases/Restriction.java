package io.harness.feature.bases;

import io.harness.feature.constants.RestrictionType;

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
      @JsonSubTypes.Type(value = EnableDisableRestriction.class, name = "AVAILABILITY")
      , @JsonSubTypes.Type(value = StaticLimitRestriction.class, name = "STATIC_LIMIT"),
          @JsonSubTypes.Type(value = RateLimitRestriction.class, name = "RATE_LIMIT"),
    })
public abstract class Restriction {
  protected RestrictionType restrictionType;

  public RestrictionType getRestrictionType() {
    return restrictionType;
  }
}
