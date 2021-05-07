package io.harness.ngtriggers.beans.source.webhook.gitlab.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum GitlabTriggerEvent { @JsonProperty("Merge Request") MERGE_REQUEST, @JsonProperty("Push") PUSH }
