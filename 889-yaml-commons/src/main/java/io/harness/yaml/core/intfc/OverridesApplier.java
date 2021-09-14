/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.core.intfc;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface OverridesApplier<T> {
  @JsonIgnore T applyOverrides(T overrideConfig);
}
