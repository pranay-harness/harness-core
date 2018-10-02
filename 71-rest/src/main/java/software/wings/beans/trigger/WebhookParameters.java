package software.wings.beans.trigger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookParameters {
  private List<String> params;
  private List<String> expressions = new ArrayList<>();

  public static final String PULL_REQUEST_ID = "${pullrequest.id}";
  public static final String PULL_REQUEST_TITLE = "${pullrequest.title}";
  public static final String SOURCE_BRANCH_NAME = "${pullrequest.fromRef.branch.name}";
  public static final String TARGET_BRANCH_NAME = "${pullrequest.toRef.branch.name}";
  public static final String SOURCE_REPOSITORY_NAME = "${pullrequest.fromRef.repository.project.name}";
  public static final String SOURCE_REPOSITORY_OWNER = "${pullrequest.fromRef.repository.owner.username}";
  public static final String DESTINATION_REPOSITORY_NAME = "${pullrequest.toRef.repository.project.name}";
  public static final String DESTINATION_REPOSITORY_OWNER = "${pullrequest.toRef.repository.owner.username}";
  public static final String SOURCE_COMMIT_HASH = "${pullrequest.fromRef.commit.hash}";
  public static final String DESTINATION_COMMIT_HASH = "${pullrequest.toRef.commit.hash}";

  // Git Hub Pull request suggestions
  public static final String GH_PR_ID = "${pull_request.id}";
  public static final String GH_PR_NUMBER = "${pull_request.number}";
  public static final String GH_PR_STATE = "${pull_request.state}";
  public static final String GH_PR_URL = "${pull_request.url}";

  // Git Hub Push event suggestions
  public static final String GH_PUSH_REF = "${ref}";
  public static final String GH_PUSH_REF_BRANCH = "${ref.split('/')[2]}";
  public static final String GH_PUSH_COMMIT_ID = "${commits[0].id}";
  public static final String GH_PUSH_HEAD_COMMIT_ID = "${head_commit.id}";
  public static final String GH_PUSH_REPOSITORY_NAME = "${repository.name}";
  public static final String GH_PUSH_REPOSITORY_ID = "${repository.id}";

  public List<String> bitBucketPullRequestExpressions() {
    List<String> prSuggestions = new ArrayList<>();
    prSuggestions.add(PULL_REQUEST_ID);
    prSuggestions.add(PULL_REQUEST_TITLE);
    prSuggestions.add(SOURCE_BRANCH_NAME);
    prSuggestions.add(TARGET_BRANCH_NAME);
    prSuggestions.add(SOURCE_REPOSITORY_NAME);
    prSuggestions.add(DESTINATION_REPOSITORY_NAME);
    prSuggestions.add(SOURCE_REPOSITORY_OWNER);
    prSuggestions.add(DESTINATION_REPOSITORY_OWNER);
    prSuggestions.add(SOURCE_COMMIT_HASH);
    prSuggestions.add(DESTINATION_COMMIT_HASH);
    return prSuggestions;
  }

  public List<String> gitHubPullRequestExpressions() {
    List<String> prSuggestions = new ArrayList<>();
    prSuggestions.add(GH_PR_ID);
    prSuggestions.add(GH_PR_NUMBER);
    prSuggestions.add(GH_PR_STATE);
    prSuggestions.add(GH_PR_URL);
    return prSuggestions;
  }

  public List<String> gitHubPushEventExpressions() {
    List<String> pushSuggestions = new ArrayList<>();
    pushSuggestions.add(GH_PUSH_REF);
    pushSuggestions.add(GH_PUSH_REF_BRANCH);
    pushSuggestions.add(GH_PUSH_COMMIT_ID);
    pushSuggestions.add(GH_PUSH_HEAD_COMMIT_ID);
    pushSuggestions.add(GH_PUSH_REPOSITORY_NAME);
    pushSuggestions.add(GH_PUSH_REPOSITORY_ID);
    return pushSuggestions;
  }

  public List<String> suggestExpressions(WebhookSource webhookSource, WebhookEventType eventType) {
    if (webhookSource == null || eventType == null) {
      return bitBucketPullRequestExpressions();
    }
    if (WebhookSource.BITBUCKET.equals(webhookSource)) {
      if (WebhookEventType.PULL_REQUEST.equals(eventType)) {
        return bitBucketPullRequestExpressions();
      }
    } else if (WebhookSource.GITHUB.equals(webhookSource)) {
      if (WebhookEventType.PULL_REQUEST.equals(eventType)) {
        return gitHubPullRequestExpressions();
      } else if (WebhookEventType.PUSH.equals(eventType)) {
        return gitHubPushEventExpressions();
      }
    }
    return new ArrayList<>();
  }
}
