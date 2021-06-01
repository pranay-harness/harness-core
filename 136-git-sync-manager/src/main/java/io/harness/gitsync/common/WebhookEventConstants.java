package io.harness.gitsync.common;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(DX)
@UtilityClass
public class WebhookEventConstants {
  public static final String GIT_CREATE_BRANCH_EVENT = "GitCreateBranchEvent";
  public static final String GIT_CREATE_BRANCH_EVENT_CONSUMER = "GitCreateBranchEventConsumer";
  public static final String GIT_PUSH_EVENT = "GitPushEvent";
  public static final String GIT_PUSH_EVENT_CONSUMER = "GitPushEventConsumer";
}
