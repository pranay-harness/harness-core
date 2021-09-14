/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.yaml;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

/**
 * Created by anubhaw on 10/27/17.
 */
@OwnedBy(HarnessTeam.DX)
@TargetModule(HarnessModule._970_API_SERVICES_BEANS)
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
    FILES_BETWEEN_COMMITS,
    FETCH_FILES_FROM_MULTIPLE_REPO
  }
}
