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

  public List<String> pullRequestExpressions() {
    expressions = new ArrayList<>();
    expressions.add(PULL_REQUEST_ID);
    expressions.add(PULL_REQUEST_TITLE);
    expressions.add(SOURCE_BRANCH_NAME);
    expressions.add(TARGET_BRANCH_NAME);
    expressions.add(SOURCE_REPOSITORY_NAME);
    expressions.add(DESTINATION_REPOSITORY_NAME);
    expressions.add(SOURCE_REPOSITORY_OWNER);
    expressions.add(DESTINATION_REPOSITORY_OWNER);
    expressions.add(SOURCE_COMMIT_HASH);
    expressions.add(DESTINATION_COMMIT_HASH);
    return expressions;
  }
}
