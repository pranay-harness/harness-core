/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.licensing.checks;

import io.harness.ModuleType;
import io.harness.licensing.Edition;
import io.harness.licensing.EditionAction;
import io.harness.licensing.entities.modules.ModuleLicense;

import java.util.Map;
import java.util.Set;

public interface LicenseComplianceResolver {
  void preCheck(ModuleLicense moduleLicense, EditionAction targetAction);
  Map<Edition, Set<EditionAction>> getEditionStates(ModuleType moduleType, String accountId);
}
