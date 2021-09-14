/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.execution;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = BranchWebhookEvent.class, name = "BRANCH")
  , @JsonSubTypes.Type(value = PRWebhookEvent.class, name = "PR")
})
public interface WebhookEvent {
  enum Type { PR, BRANCH }
  WebhookEvent.Type getType();
}
