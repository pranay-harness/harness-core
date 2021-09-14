/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.manifests.request;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;

import java.util.Set;

@OwnedBy(CDC)
public interface ManifestCollectionParams extends TaskParameters, ExecutionCapabilityDemander {
  String getAccountId();
  String getAppId();
  String getAppManifestId();
  String getServiceId();
  Set<String> getPublishedVersions();
}
