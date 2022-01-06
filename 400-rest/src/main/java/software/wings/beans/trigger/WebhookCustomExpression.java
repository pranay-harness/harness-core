/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.trigger;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public enum WebhookCustomExpression {
  BIT_BUCKET_PULL_REQUEST_ID("Pull Request Id", "${pullrequest.id}", WebhookSource.BITBUCKET),
  BIT_BUCKET_PULL_REQUEST_TITLE("Pull Request Title", "${pullrequest.title}", WebhookSource.BITBUCKET),
  BIT_BUCKET_SOURCE_BRANCH_NAME("Source Branch Name", "${pullrequest.fromRef.branch.name}", WebhookSource.BITBUCKET),
  BIT_BUCKET_TARGET_BRANCH_NAME("Target Branch Name", "${pullrequest.toRef.branch.name}", WebhookSource.BITBUCKET),
  BIT_BUCKET_SOURCE_REPOSITORY_NAME(
      "Source Repository Name", "${pullrequest.fromRef.repository.project.name}", WebhookSource.BITBUCKET),
  BIT_BUCKET_SOURCE_REPOSITORY_OWNER(
      "Source Repository Owner", "${pullrequest.fromRef.repository.owner.username}", WebhookSource.BITBUCKET),
  BIT_BUCKET_DESTINATION_REPOSITORY_NAME(
      "Destination Repository Name", "${pullrequest.toRef.repository.project.name}", WebhookSource.BITBUCKET),
  BIT_BUCKET_DESTINATION_REPOSITORY_OWNER(
      "Destination Repository OWNER", "${pullrequest.toRef.repository.owner.username}", WebhookSource.BITBUCKET),
  BIT_BUCKET_SOURCE_COMMIT_HASH("Source Commit Hash", "${pullrequest.fromRef.commit.hash}", WebhookSource.BITBUCKET),
  BIT_BUCKET_DESTINATION_COMMIT_HASH(
      "Destination Commit Hash", "${pullrequest.toRef.commit.hash}", WebhookSource.BITBUCKET),

  // Bit Bucket Push Request suggestions
  BIT_BUCKET_PUSH_BRANCH_REF("Push Branch Reference", "${push.changes[0].'new'.name}", WebhookSource.BITBUCKET),
  BIT_BUCKET_PULL_BRANCH_REF("Pull Branch Reference", "${pullrequest.source.branch.name}", WebhookSource.BITBUCKET),
  BIT_BUCKET_COMMIT_ID("Commit Id", "${push.changes[0].'new'.target.hash}", WebhookSource.BITBUCKET),
  BIT_BUCKET_REFS_CHANGED_REF(
      "Reference_Changed_Ref", "${changes[0].refId.split('refs/heads/')[1]}", WebhookSource.BITBUCKET),

  // Git Hub Pull request suggestions
  GH_PR_ID("PR Id", "${pull_request.id}", WebhookSource.GITHUB),
  GH_PR_NUMBER("PR Number", "${pull_request.number}", WebhookSource.GITHUB),
  GH_PR_STATE("PR State", "${pull_request.state}", WebhookSource.GITHUB),
  GH_PR_URL("PR URL", "${pull_request.url}", WebhookSource.GITHUB),

  // Git Hub Push event suggestions
  GH_PUSH_REF("Push Reference", "${ref}", WebhookSource.GITHUB),
  GH_PUSH_REF_BRANCH("Push Reference Branch", "${ref.split('refs/heads/')[1]}", WebhookSource.GITHUB),
  GH_PULL_REF_BRANCH("Pull Reference Branch", "${pull_request.head.ref}", WebhookSource.GITHUB),
  GH_PUSH_COMMIT_ID("Push Commit Id", "${commits[0].id}", WebhookSource.GITHUB),
  GH_PUSH_HEAD_COMMIT_ID("Push Head Commit Id", "${head_commit.id}", WebhookSource.GITHUB),
  GH_PUSH_REPOSITORY_NAME("Repository Name", "${repository.name}", WebhookSource.GITHUB),
  GH_PUSH_REPOSITORY_ID("Repository Id", "${repository.id}", WebhookSource.GITHUB),

  // Git Lab Push Event Suggestions
  GIT_LAB_PUSH_REF("Push Reference", "${ref}", WebhookSource.GITLAB),
  GIT_LAB_PUSH_REF_BRANCH("Push Reference Branch", "${ref.split('refs/heads/')[1]}", WebhookSource.GITLAB),
  GIT_LAB_PULL_REF_BRANCH("Pull Reference Branch", "${object_attributes.source_branch}", WebhookSource.GITLAB),
  GIT_LAB_PUSH_COMMIT_ID("Push Commit Id", "${checkout_sha}", WebhookSource.GITLAB),
  GIT_LAB_PUSH_REPOSITORY_NAME("Push Repository Name", "${repository.name}", WebhookSource.GITLAB),
  GIT_LAB_PUSH_REPOSITORY_ID("Push Repository Id", "${repository.id}", WebhookSource.GITLAB);

  @Getter private String value;
  @Getter private String displayName;
  private WebhookSource type;

  WebhookCustomExpression(String displayName, String value, WebhookSource type) {
    this.value = value;
    this.displayName = displayName;
    this.type = type;
  }

  public static Map<String, String> suggestExpressions(String webhookSource) {
    if (webhookSource == null) {
      return null;
    }
    return Arrays.stream(WebhookCustomExpression.values())
        .filter(webhookCustomExpression -> webhookCustomExpression.type == WebhookSource.valueOf(webhookSource))
        .collect(Collectors.toMap(WebhookCustomExpression::getDisplayName, WebhookCustomExpression::getValue));
  }
}
