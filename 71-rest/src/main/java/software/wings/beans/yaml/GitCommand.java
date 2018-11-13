package software.wings.beans.yaml;

/**
 * Created by anubhaw on 10/27/17.
 */
public interface GitCommand {
  GitCommandType getGitCommandType();

  enum GitCommandType {
    CLONE,
    CHECKOUT,
    DIFF,
    COMMIT,
    PUSH,
    PULL,
    COMMIT_AND_PUSH,
    FETCH_FILES,
    VALIDATE,
    FILES_BETWEEN_COMMITS
  }
}
