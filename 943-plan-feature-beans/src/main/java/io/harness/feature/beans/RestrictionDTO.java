package io.harness.feature.beans;

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
  @JsonSubTypes.Type(value = EnableDisableRestrictionDTO.class, name = "AVAILABILITY")
  , @JsonSubTypes.Type(value = StaticLimitRestrictionDTO.class, name = "STATIC_LIMIT"),
      @JsonSubTypes.Type(value = RateLimitRestrictionDTO.class, name = "RATE_LIMIT"),
})
public abstract class RestrictionDTO {}
