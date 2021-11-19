package io.harness.cvng.servicelevelobjective.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SLIMetricType { @JsonProperty("Threshold") THRESHOLD, @JsonProperty("Ratio") RATIO }
