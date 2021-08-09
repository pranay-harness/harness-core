package io.harness.cf;

import java.util.List;

class RemoveSegmentToVariationMapParams {
  String variation;
  List<String> segments;

  RemoveSegmentToVariationMapParams(String variation, List<String> segments) {
    this.variation = variation;
    this.segments = segments;
  }
}
