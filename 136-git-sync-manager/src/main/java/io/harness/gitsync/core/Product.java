package io.harness.gitsync.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Product { @JsonProperty("cd") CD, @JsonProperty("ci") CI, @JsonProperty("core") CORE }
