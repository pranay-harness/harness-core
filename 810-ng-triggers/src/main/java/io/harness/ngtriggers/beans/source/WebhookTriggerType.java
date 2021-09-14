/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.beans.source;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.Constants.AWS_CODECOMMIT_REPO;
import static io.harness.ngtriggers.Constants.BITBUCKET_REPO;
import static io.harness.ngtriggers.Constants.CUSTOM_REPO;
import static io.harness.ngtriggers.Constants.GITHUB_REPO;
import static io.harness.ngtriggers.Constants.GITLAB_REPO;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("ngTriggerType")
@OwnedBy(PIPELINE)
public enum WebhookTriggerType {
  @JsonProperty(GITHUB_REPO) GITHUB(GITHUB_REPO, "GITHUB"),
  @JsonProperty(GITLAB_REPO) GITLAB(GITLAB_REPO, "GITLAB"),
  @JsonProperty(BITBUCKET_REPO) BITBUCKET(BITBUCKET_REPO, "BITBUCKET"),
  @JsonProperty(CUSTOM_REPO) CUSTOM(CUSTOM_REPO, "CUSTOM"),
  @JsonProperty(AWS_CODECOMMIT_REPO) AWS_CODECOMMIT(AWS_CODECOMMIT_REPO, "AWS_CODECOMMIT");

  private String value;
  private String entityMetadataName;

  WebhookTriggerType(String value, String entityMetadataName) {
    this.value = value;
    this.entityMetadataName = entityMetadataName;
  }

  public String getValue() {
    return value;
  }

  public String getEntityMetadataName() {
    return entityMetadataName;
  }
}
