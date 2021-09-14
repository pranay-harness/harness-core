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
public interface OwnedByInfrastructureDefinition {
  /**
   * Prune if belongs to infrastructure definition.
   *
   * @param appId the app id
   * @param infraDefinitionId the infrastructure definition id
   */
  void pruneByInfrastructureDefinition(String appId, String infraDefinitionId);
}
