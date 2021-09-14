/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
