package io.harness.cvng.servicelevelobjective.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DayOfWeek {
  @JsonProperty("Mon") MONDAY,
  @JsonProperty("Tue") TUESDAY,
  @JsonProperty("Wed") WEDNESDAY,
  @JsonProperty("Thu") THURSDAY,
  @JsonProperty("Fri") FRIDAY,
  @JsonProperty("Sat") SATURDAY,
  @JsonProperty("Sun") SUNDAY;
}
