package io.harness.ngtriggers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface Constants {
  String PR = "PR";
  String PUSH = "PUSH";

  String PULL_REQUEST_EVENT_TYPE = "Pull Request";
  String MERGE_REQUEST_EVENT_TYPE = "Merge Request";
  String PUSH_EVENT_TYPE = "Push";
  String ISSUE_COMMENT_EVENT_TYPE = "Issue Comment";

  String TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER = ":";
  String TRIGGER_REF_DELIMITER = "/";
  String TRIGGER_REF = "triggerRef";
  String EVENT_CORRELATION_ID = "eventCorrelationId";

  String BRANCH = "branch";
  String TARGET_BRANCH = "targetBranch";
  String SOURCE_BRANCH = "sourceBranch";
  String EVENT = "event";
  String PR_NUMBER = "prNumber";
  String COMMIT_SHA = "commitSha";
  String TYPE = "type";
  String PAYLOAD = "payload";
  String EVENT_PAYLOAD = "eventPayload";
  String HEADER = "header";
  String REPO_URL = "repoUrl";
  String GIT_USER = "gitUser";

  String WEBHOOK_TYPE = "WEBHOOK";
  String SCHEDULED_TYPE = "SCHEDULED";
  String CUSTOM_TYPE = "CUSTOM";
}
