package io.harness;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum Microservice {
  @JsonProperty("CD") CD,
  @JsonProperty("CI") CI,
  @JsonProperty("CORE") CORE,
  @JsonProperty("CV") CV,
  @JsonProperty("CF") CF,
  @JsonProperty("CE") CE,
  @JsonProperty("PMS") PMS,
  @JsonProperty("ACCESSCONTROL") ACCESSCONTROL,
  @JsonProperty("TEMPLATESERVICE") TEMPLATESERVICE;

  @JsonCreator
  public static Microservice fromString(String microservice) {
    for (Microservice msvc : Microservice.values()) {
      if (msvc.name().equalsIgnoreCase(microservice)) {
        return msvc;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + microservice);
  }
}