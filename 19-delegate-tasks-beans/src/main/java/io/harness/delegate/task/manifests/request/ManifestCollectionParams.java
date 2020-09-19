package io.harness.delegate.task.manifests.request;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;

@OwnedBy(CDC)
public interface ManifestCollectionParams extends TaskParameters, ExecutionCapabilityDemander {
  String getAccountId();
  String getAppId();
  String getAppManifestId();
}
