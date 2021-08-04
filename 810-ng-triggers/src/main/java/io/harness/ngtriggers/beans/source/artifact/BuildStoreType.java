package io.harness.ngtriggers.beans.source.artifact;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import org.codehaus.jackson.annotate.JsonProperty;

@OwnedBy(PIPELINE) public enum BuildStoreType { @JsonProperty("Http") HTTP }