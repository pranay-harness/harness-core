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
public interface OwnedByInfrastructureMapping {
  /**
   * Prune if belongs to infrastructure mapping.
   *
   * @param appId the app id
   * @param infrastructureMappingId the infrastructure mapping id
   */
  void pruneByInfrastructureMapping(String appId, String infrastructureMappingId);
}
