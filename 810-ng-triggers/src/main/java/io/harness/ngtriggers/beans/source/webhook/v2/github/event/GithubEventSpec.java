/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.beans.source.webhook.v2.github.event;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.Constants.ISSUE_COMMENT_EVENT_TYPE;
import static io.harness.ngtriggers.Constants.PULL_REQUEST_EVENT_TYPE;
import static io.harness.ngtriggers.Constants.PUSH_EVENT_TYPE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAware;
import io.harness.ngtriggers.beans.source.webhook.v2.git.PayloadAware;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GithubPRSpec.class, name = PULL_REQUEST_EVENT_TYPE)
  , @JsonSubTypes.Type(value = GithubPushSpec.class, name = PUSH_EVENT_TYPE),
      @JsonSubTypes.Type(value = GithubIssueCommentSpec.class, name = ISSUE_COMMENT_EVENT_TYPE)
})
@OwnedBy(PIPELINE)
public interface GithubEventSpec extends PayloadAware, GitAware {}
