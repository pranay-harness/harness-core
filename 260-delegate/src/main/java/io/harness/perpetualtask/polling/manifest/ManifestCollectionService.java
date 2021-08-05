package io.harness.perpetualtask.polling.manifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;

import java.util.List;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.CDC)
public interface ManifestCollectionService {
  List<String> collectManifests(@NotNull ManifestDelegateConfig params);

  void cleanup(ManifestDelegateConfig params);
}
