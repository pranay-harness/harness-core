package io.harness.ngtriggers.beans.source.webhook;

public interface RepoSpec {
  default String getIdentifier() {
    throw new UnsupportedOperationException();
  }

  default String getRepoName() {
    throw new UnsupportedOperationException();
  }
}
