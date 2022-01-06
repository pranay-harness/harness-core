/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
