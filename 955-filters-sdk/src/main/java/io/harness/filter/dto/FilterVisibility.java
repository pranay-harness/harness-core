package io.harness.filter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FilterVisibility { @JsonProperty("EveryOne") EVERYONE, @JsonProperty("OnlyCreator") ONLY_CREATOR }
