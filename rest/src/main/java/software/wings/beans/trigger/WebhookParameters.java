package software.wings.beans.trigger;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WebhookParameters {
  private List<String> params;
  private List<String> expressions = new ArrayList<>();

  public static final String SOURCE_BRANCH_NAME = "${source.branch.name}";
  public static final String TARGET_BRANCH_NAME = "${destination.branch.name}";
  public static final String SOURCE_REPOSITORY_NAME = "${source.repository.name}";
  //  public static final String SOURCE_REPOSITORY_OWNER = "${sourceRepositoryOwner}";
  public static final String PULL_REQUEST_ID = "${id}";
  //  public static final String DESTINATION_REPOSITORY_OWNER_MAPPING_NAME = "${destinationRepositoryOwner}";
  public static final String DESTINATION_REPOSITORY_NAME = "${destination.repository.name}  ";
  public static final String PULL_REQUEST_TITLE = "${title}";
  public static final String SOURCE_COMMIT_HASH = "${source.commit.hash}";
  public static final String DESTINATION_COMMIT_HASH = "${destination.commit.hash}";

  public List<String> pullRequestExpressions() {
    expressions = new ArrayList<>();
    expressions.add(SOURCE_BRANCH_NAME);
    expressions.add(TARGET_BRANCH_NAME);
    expressions.add(SOURCE_REPOSITORY_NAME);
    expressions.add(PULL_REQUEST_ID);
    expressions.add(DESTINATION_REPOSITORY_NAME);
    expressions.add(PULL_REQUEST_TITLE);
    expressions.add(SOURCE_COMMIT_HASH);
    return expressions;
  }
}
