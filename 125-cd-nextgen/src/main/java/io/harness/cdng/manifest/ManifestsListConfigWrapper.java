package io.harness.cdng.manifest;

import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;

public class ManifestsListConfigWrapper implements Visitable {
  List<ManifestConfigWrapper> manifests;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ManifestsListConfigWrapper(List<ManifestConfigWrapper> manifests) {
    this.manifests = manifests;
  }
}
