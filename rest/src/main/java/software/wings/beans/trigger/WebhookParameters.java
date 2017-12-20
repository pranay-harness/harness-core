package software.wings.beans.trigger;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WebhookParameters {
  private List<String> params;
  private List<String> expressions = new ArrayList<>();

  public static final String SOURCE_BRANCH = "${sourceBranch}";
  public static final String TARGET_BRANCH = "${targetBranch}";
  public static final String SOURCE_REPOSITORY_NAME = "${sourceRepositoryName}";
  public static final String SOURCE_REPOSITORY_OWNER = "${sourceRepositoryOwner}";
  public static final String PULL_REQUEST_ID = "${pullRequestId}";
  public static final String DESTINATION_REPOSITORY_OWNER = "${destinationRepositoryOwner}";
  public static final String DESTINATION_REPOSITORY_NAME = "${distinationRepositoryName}";
  public static final String PULL_REQUEST_TITLE = "${pullRequestTitle}";
  public static final String SOURCE_COMMIT_HASH = "${sourceCommitHash}";

  public static final String SOURCE_BRANCH_MAPPING_NAME = "source.branch.name";
  public static final String TARGET_BRANCH_MAPPING_NAME = "destination.branch.name";
  public static final String SOURCE_REPOSITORY_MAPPING_NAME = "source.repository.name";
  public static final String SOURCE_REPOSITORY_OWNER_MAPPING_NAME = "${sourceRepositoryOwner}";
  public static final String PULL_REQUEST_ID_MAPPING_NAME = "${pullRequestId}";
  public static final String DESTINATION_REPOSITORY_OWNER_MAPPING_NAME = "${destinationRepositoryOwner}";
  public static final String DESTINATION_REPOSITORY_NAME_MAPPING_NAME = "destination.repository.name";
  public static final String PULL_REQUEST_TITLE_MAPPING_NAME = "${pullRequestTitle}";
  public static final String SOURCE_COMMIT_HASH_MAPPING_NAME = "${sourceCommitHash}";

  public List<String> pullRequestExpressions() {
    expressions = new ArrayList<>();
    expressions.add(SOURCE_BRANCH);
    expressions.add(TARGET_BRANCH);
    expressions.add(SOURCE_REPOSITORY_NAME);
    expressions.add(SOURCE_REPOSITORY_OWNER);
    expressions.add(PULL_REQUEST_ID);
    expressions.add(DESTINATION_REPOSITORY_OWNER);
    expressions.add(DESTINATION_REPOSITORY_NAME);
    expressions.add(PULL_REQUEST_TITLE);
    expressions.add(SOURCE_COMMIT_HASH);
    return expressions;
  }
}
