package io.harness.delegate.beans.git;

public enum GitCommandType {
  CLONE,
  CHECKOUT,
  DIFF,
  COMMIT,
  PUSH,
  PULL,
  COMMIT_AND_PUSH,
  FETCH_FILES,
  VALIDATE,
  FILES_BETWEEN_COMMITS,
  FETCH_FILES_FROM_MULTIPLE_REPO
}
