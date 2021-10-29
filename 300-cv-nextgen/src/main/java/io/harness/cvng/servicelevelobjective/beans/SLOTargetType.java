package io.harness.cvng.servicelevelobjective.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SLOTargetType { @JsonProperty("Rolling") ROLLING, @JsonProperty("Calender") CALENDER }
