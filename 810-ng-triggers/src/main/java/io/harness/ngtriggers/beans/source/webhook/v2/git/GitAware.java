/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.beans.source.webhook.v2.git;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(PIPELINE)
public interface GitAware {
  // There is a rason im naming these apis as fetch_ and not get_
  // if these are names as get_, it actually becomes easy, as impl classes already have getter methods.

  // But, then these attributes, start appearing as a part of this interface type in yaml schema.
  // This creates issues in auto-suggest. e.g. in GithubPush trigger, we don't want to show actions, as
  // there are no actions for push. If we Use get_ here, that wont be possible.
  String fetchConnectorRef();

  String fetchRepoName();

  GitEvent fetchEvent();

  List<GitAction> fetchActions();

  default boolean fetchAutoAbortPreviousExecutions() {
    return false;
  }
}
