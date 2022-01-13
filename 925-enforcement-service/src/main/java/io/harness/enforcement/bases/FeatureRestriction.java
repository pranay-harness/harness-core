/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.bases;

import io.harness.ModuleType;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.licensing.Edition;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@AllArgsConstructor
@Slf4j
public class FeatureRestriction {
  private FeatureRestrictionName name;
  private String description;
  private ModuleType moduleType;
  private Map<Edition, Restriction> restrictions;

  public void setModuleType(ModuleType moduleType) {
    this.moduleType = moduleType;
  }
}
