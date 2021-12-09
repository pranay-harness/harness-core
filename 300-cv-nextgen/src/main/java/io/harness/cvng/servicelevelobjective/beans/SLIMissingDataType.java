package io.harness.cvng.servicelevelobjective.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SLIMissingDataType { @JsonProperty("Good") GOOD, @JsonProperty("Bad") BAD, @JsonProperty("Ignore") IGNORE }
