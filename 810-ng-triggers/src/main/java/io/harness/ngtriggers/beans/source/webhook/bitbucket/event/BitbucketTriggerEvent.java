package io.harness.ngtriggers.beans.source.webhook.bitbucket.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum BitbucketTriggerEvent { @JsonProperty("Pull Request") PULL_REQUEST, @JsonProperty("Push") PUSH }
