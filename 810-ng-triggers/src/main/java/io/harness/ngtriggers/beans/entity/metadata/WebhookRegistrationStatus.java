package io.harness.ngtriggers.beans.entity.metadata;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE) public enum WebhookRegistrationStatus { SUCCESS, FAILED, ERROR, TIMEOUT, UNAVAILABLE }
