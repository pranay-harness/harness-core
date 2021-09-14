/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.ownership;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface OwnedByEnvironment {
  /**
   * Prune if belongs to environment.
   *
   * @param appId the app id
   * @param envId the environment id
   */
  void pruneByEnvironment(String appId, String envId);
}
