package io.harness.ngtriggers.beans.source.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum WebhookAction { @JsonProperty("open") OPEN, @JsonProperty("closed") CLOSED }