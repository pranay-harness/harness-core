/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
