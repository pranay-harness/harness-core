/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(DX)
@UtilityClass
public class WebhookEventConstants {
  public static final String GIT_BRANCH_HOOK_EVENT = "GitBranchHookEvent";
  public static final String GIT_BRANCH_HOOK_EVENT_CONSUMER = "GitBranchHookEventConsumer";
  public static final String GIT_PUSH_EVENT = "GitPushEvent";
  public static final String GIT_PUSH_EVENT_CONSUMER = "GitPushEventConsumer";
}
