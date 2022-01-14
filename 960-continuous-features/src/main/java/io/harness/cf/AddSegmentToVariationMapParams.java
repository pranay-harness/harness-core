package io.harness.cf;

import java.util.List;

class AddSegmentToVariationMapParams {
  String variation;
  List<String> segments;

  AddSegmentToVariationMapParams(String variation, List<String> segments) {
    this.variation = variation;
    this.segments = segments;
  }
}
