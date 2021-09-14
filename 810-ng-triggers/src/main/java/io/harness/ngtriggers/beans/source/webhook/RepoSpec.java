/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.beans.source.webhook;

public interface RepoSpec {
  default String getIdentifier() {
    throw new UnsupportedOperationException();
  }

  default String getRepoName() {
    throw new UnsupportedOperationException();
  }
}
