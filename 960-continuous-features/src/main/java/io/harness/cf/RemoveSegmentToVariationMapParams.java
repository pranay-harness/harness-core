/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
