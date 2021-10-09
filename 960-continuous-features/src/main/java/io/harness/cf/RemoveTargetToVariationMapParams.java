package io.harness.cf;

import java.util.List;

class RemoveTargetToVariationMapParams {
  String variation;
  List<String> targets;

  RemoveTargetToVariationMapParams(String variation, List<String> targets) {
    this.variation = variation;
    this.targets = targets;
  }
}
