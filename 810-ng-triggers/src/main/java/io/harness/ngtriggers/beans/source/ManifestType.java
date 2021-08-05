package io.harness.ngtriggers.beans.source;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PIPELINE) public enum ManifestType { @JsonProperty("HelmChart") HELM_MANIFEST }
