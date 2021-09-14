/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.ci;

import io.harness.delegate.beans.ci.pod.ConnectorDetails;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class CIBuildStatusPushParameters extends CIBuildPushParameters {
  private String title;
  private String desc;
  private String state;

  @Builder
  public CIBuildStatusPushParameters(String buildNumber, String detailsUrl, String repo, String owner, String sha,
      String identifier, String target_url, String key, String installId, String appId, String title, String desc,
      String state, String token, String userName, GitSCMType gitSCMType, ConnectorDetails connectorDetails) {
    super(buildNumber, detailsUrl, repo, owner, sha, identifier, target_url, key, installId, appId, token, userName,
        gitSCMType, connectorDetails);
    this.title = title;
    this.desc = desc;
    this.state = state;
    this.commandType = CIBuildPushTaskType.STATUS;
  }

  @Override
  public CIBuildPushTaskType getCommandType() {
    return commandType;
  }
}
