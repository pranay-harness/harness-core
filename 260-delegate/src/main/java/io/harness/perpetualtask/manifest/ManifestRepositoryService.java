package io.harness.perpetualtask.manifest;

import io.harness.delegate.task.manifests.request.ManifestCollectionParams;

import software.wings.beans.appmanifest.HelmChart;

import java.util.List;
import javax.validation.constraints.NotNull;

public interface ManifestRepositoryService {
  List<HelmChart> collectManifests(@NotNull ManifestCollectionParams params) throws Exception;

  void cleanup(@NotNull ManifestCollectionParams params) throws Exception;
}
