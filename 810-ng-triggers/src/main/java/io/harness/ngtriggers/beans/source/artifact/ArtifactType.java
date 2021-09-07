package io.harness.ngtriggers.beans.source.artifact;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PIPELINE) public enum ArtifactType { @JsonProperty("GCR") GCR }
