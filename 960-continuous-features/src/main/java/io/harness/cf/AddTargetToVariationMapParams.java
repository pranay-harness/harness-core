package io.harness.cf;

import java.util.List;

class AddTargetToVariationMapParams {
  String variation;
  List<String> targets;

  AddTargetToVariationMapParams(String variation, List<String> targets) {
    this.variation = variation;
    this.targets = targets;
  }
}
