package software.wings.beans.trigger;

import io.harness.exception.WingsException;

import java.util.HashMap;
import java.util.Map;

public enum WebhookCustomExpression {
  BIT_BUCKET_PULL_REQUEST_ID("Pull Request Id", "${pullrequest.id}"),
  BIT_BUCKET_PULL_REQUEST_TITLE("Pull Request Title", "${pullrequest.title}"),
  BIT_BUCKET_SOURCE_BRANCH_NAME("Source Branch Name", "${pullrequest.fromRef.branch.name}"),
  BIT_BUCKET_TARGET_BRANCH_NAME("Target Branch Name", "${pullrequest.toRef.branch.name}"),
  BIT_BUCKET_SOURCE_REPOSITORY_NAME("Source Repository Name", "${pullrequest.fromRef.repository.project.name}"),
  BIT_BUCKET_SOURCE_REPOSITORY_OWNER("Source Repository Owner", "${pullrequest.fromRef.repository.owner.username}"),
  BIT_BUCKET_DESTINATION_REPOSITORY_NAME("Destination Repository Name", "${pullrequest.toRef.repository.project.name}"),
  BIT_BUCKET_DESTINATION_REPOSITORY_OWNER(
      "Destination Repository OWNER", "${pullrequest.toRef.repository.owner.username}"),
  BIT_BUCKET_SOURCE_COMMIT_HASH("Source Commit Hash", "${pullrequest.fromRef.commit.hash}"),
  BIT_BUCKET_DESTINATION_COMMIT_HASH("Destination Commit Hash", "${pullrequest.toRef.commit.hash}"),

  // Bit Bucket Push Request suggestions
  BIT_BUCKET_PUSH_BRANCH_REF("Push Branch Reference", "${push.changes[0].'new'.name}"),
  BIT_BUCKET_PULL_BRANCH_REF("Pull Branch Reference", "${pullrequest.source.branch.name}"),
  BIT_BUCKET_COMMIT_ID("Commit Id", "${push.changes[0].'new'.target.hash}"),
  BIT_BUCKET_REFS_CHANGED_REF("Reference_Changed_Ref", "${changes[0].refId.split('refs/heads/')[1]}"),

  // Git Hub Pull request suggestions
  GH_PR_ID("PR Id", "${pull_request.id}"),
  GH_PR_NUMBER("PR Number", "${pull_request.number}"),
  GH_PR_STATE("PR State", "${pull_request.state}"),
  GH_PR_URL("PR URL", "${pull_request.url}"),

  // Git Hub Push event suggestions
  GH_PUSH_REF("Push Reference", "${ref}"),
  GH_PUSH_REF_BRANCH("Push Reference Branch", "${ref.split('refs/heads/')[1]}"),
  GH_PULL_REF_BRANCH("Pull Reference Branch", "${pull_request.head.ref}"),
  GH_PUSH_COMMIT_ID("Push Commit Id", "${commits[0].id}"),
  GH_PUSH_HEAD_COMMIT_ID("Push Head Commit Id", "${head_commit.id}"),
  GH_PUSH_REPOSITORY_NAME("Repository Name", "${repository.name}"),
  GH_PUSH_REPOSITORY_ID("Repository Id", "${repository.id}"),

  // Git Lab Push Event Suggestions
  GIT_LAB_PUSH_REF("Push Reference", "${ref}"),
  GIT_LAB_PUSH_REF_BRANCH("Push Reference Branch", "${ref.split('refs/heads/')[1]}"),
  GIT_LAB_PULL_REF_BRANCH("Pull Reference Branch", "${object_attributes.source_branch}"),
  GIT_LAB_PUSH_COMMIT_ID("Push Commit Id", "${checkout_sha}"),
  GIT_LAB_PUSH_REPOSITORY_NAME("Push Repository Name", "${repository.name}"),
  GIT_LAB_PUSH_REPOSITORY_ID("Push Repository Id", "${repository.id}");

  private String value;
  private String displayName;
  WebhookCustomExpression(String displayName, String value) {
    this.value = value;
    this.displayName = displayName;
  }

  public static Map<String, String> bitBucketExpressions() {
    Map<String, String> prSuggestions = new HashMap<>();
    prSuggestions.put(BIT_BUCKET_PULL_REQUEST_ID.displayName, BIT_BUCKET_PULL_REQUEST_ID.value);
    prSuggestions.put(BIT_BUCKET_PULL_REQUEST_TITLE.displayName, BIT_BUCKET_PULL_REQUEST_TITLE.value);
    prSuggestions.put(BIT_BUCKET_SOURCE_BRANCH_NAME.displayName, BIT_BUCKET_SOURCE_BRANCH_NAME.value);
    prSuggestions.put(BIT_BUCKET_TARGET_BRANCH_NAME.displayName, BIT_BUCKET_TARGET_BRANCH_NAME.value);
    prSuggestions.put(BIT_BUCKET_SOURCE_REPOSITORY_NAME.displayName, BIT_BUCKET_SOURCE_REPOSITORY_NAME.value);
    prSuggestions.put(BIT_BUCKET_SOURCE_REPOSITORY_OWNER.displayName, BIT_BUCKET_SOURCE_REPOSITORY_OWNER.value);
    prSuggestions.put(BIT_BUCKET_DESTINATION_REPOSITORY_NAME.displayName, BIT_BUCKET_DESTINATION_REPOSITORY_NAME.value);
    prSuggestions.put(
        BIT_BUCKET_DESTINATION_REPOSITORY_OWNER.displayName, BIT_BUCKET_DESTINATION_REPOSITORY_OWNER.value);
    prSuggestions.put(BIT_BUCKET_SOURCE_COMMIT_HASH.displayName, BIT_BUCKET_SOURCE_COMMIT_HASH.value);
    prSuggestions.put(BIT_BUCKET_DESTINATION_COMMIT_HASH.displayName, BIT_BUCKET_DESTINATION_COMMIT_HASH.value);
    prSuggestions.put(BIT_BUCKET_PUSH_BRANCH_REF.displayName, BIT_BUCKET_PUSH_BRANCH_REF.value);
    prSuggestions.put(BIT_BUCKET_PULL_BRANCH_REF.displayName, BIT_BUCKET_PULL_BRANCH_REF.value);
    prSuggestions.put(BIT_BUCKET_COMMIT_ID.displayName, BIT_BUCKET_COMMIT_ID.value);
    prSuggestions.put(BIT_BUCKET_REFS_CHANGED_REF.displayName, BIT_BUCKET_REFS_CHANGED_REF.value);

    return prSuggestions;
  }

  public static Map<String, String> gitHubExpressions() {
    Map<String, String> prSuggestions = new HashMap<>();
    prSuggestions.put(GH_PR_ID.displayName, GH_PR_ID.value);
    prSuggestions.put(GH_PR_NUMBER.displayName, GH_PR_NUMBER.value);
    prSuggestions.put(GH_PR_STATE.displayName, GH_PR_STATE.value);
    prSuggestions.put(GH_PR_URL.displayName, GH_PR_URL.value);
    prSuggestions.put(GH_PUSH_REF.displayName, GH_PUSH_REF.value);
    prSuggestions.put(GH_PUSH_REF_BRANCH.displayName, GH_PUSH_REF_BRANCH.value);
    prSuggestions.put(GH_PULL_REF_BRANCH.displayName, GH_PULL_REF_BRANCH.value);
    prSuggestions.put(GH_PUSH_COMMIT_ID.displayName, GH_PUSH_COMMIT_ID.value);
    prSuggestions.put(GH_PUSH_HEAD_COMMIT_ID.displayName, GH_PUSH_HEAD_COMMIT_ID.value);
    prSuggestions.put(GH_PUSH_REPOSITORY_NAME.displayName, GH_PUSH_REPOSITORY_NAME.value);
    prSuggestions.put(GH_PUSH_REPOSITORY_ID.displayName, GH_PUSH_REPOSITORY_ID.value);

    return prSuggestions;
  }

  public static Map<String, String> gitLasbExpressions() {
    Map<String, String> prSuggestions = new HashMap<>();
    prSuggestions.put(GIT_LAB_PUSH_REF.displayName, GIT_LAB_PUSH_REF.value);
    prSuggestions.put(GIT_LAB_PUSH_REF_BRANCH.displayName, GIT_LAB_PUSH_REF_BRANCH.value);
    prSuggestions.put(GIT_LAB_PULL_REF_BRANCH.displayName, GIT_LAB_PULL_REF_BRANCH.value);
    prSuggestions.put(GIT_LAB_PUSH_COMMIT_ID.displayName, GIT_LAB_PUSH_COMMIT_ID.value);
    prSuggestions.put(GIT_LAB_PUSH_REPOSITORY_NAME.displayName, GIT_LAB_PUSH_REPOSITORY_NAME.value);
    prSuggestions.put(GIT_LAB_PUSH_REPOSITORY_ID.displayName, GIT_LAB_PUSH_REPOSITORY_ID.value);

    return prSuggestions;
  }

  public static Map<String, String> suggestExpressions(String webhookSource) {
    if (webhookSource == null) {
      return null;
    }
    if (WebhookSource.BITBUCKET.name().equals(webhookSource)) {
      return bitBucketExpressions();
    } else if (WebhookSource.GITHUB.name().equals(webhookSource)) {
      return gitHubExpressions();
    } else if (WebhookSource.GITLAB.name().equals(webhookSource)) {
      return gitLasbExpressions();
    } else {
      throw new WingsException("Webhook source " + webhookSource + " is not supported");
    }
  }
}
