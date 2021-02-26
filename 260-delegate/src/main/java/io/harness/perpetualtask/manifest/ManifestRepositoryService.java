package io.harness.perpetualtask.manifest;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.manifests.request.ManifestCollectionParams;

import software.wings.beans.appmanifest.HelmChart;

import java.util.List;
import javax.validation.constraints.NotNull;

@TargetModule(Module._420_DELEGATE_AGENT)
public interface ManifestRepositoryService {
  List<HelmChart> collectManifests(@NotNull ManifestCollectionParams params) throws Exception;

  void cleanup(@NotNull ManifestCollectionParams params) throws Exception;
}
