/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
