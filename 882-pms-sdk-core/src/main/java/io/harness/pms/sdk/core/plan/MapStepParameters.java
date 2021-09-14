/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.plan;

import io.harness.annotation.RecasterAlias;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.HashMap;

@RecasterAlias("io.harness.pms.sdk.core.plan.MapStepParameters")
public class MapStepParameters extends HashMap<String, Object> implements StepParameters {
  public MapStepParameters() {}

  public MapStepParameters(String key, Object value) {
    put(key, value);
  }
}
