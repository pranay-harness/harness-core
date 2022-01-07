/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
          @JsonSubTypes.Type(value = LicenseRateLimitRestriction.class, name = "LICENSE_RATE_LIMIT"),
          @JsonSubTypes.Type(value = LicenseStaticLimitRestriction.class, name = "LICENSE_STATIC_LIMIT"),
    })
public abstract class Restriction {
  protected RestrictionType restrictionType;

  public RestrictionType getRestrictionType() {
    return restrictionType;
  }
}
