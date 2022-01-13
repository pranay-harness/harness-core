/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.utils.monitoredService;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceSpec;
import io.harness.cvng.core.entities.CVConfig;

import com.google.common.base.Preconditions;
import java.util.List;

public interface CVConfigToHealthSourceTransformer<C extends CVConfig, T extends HealthSourceSpec> {
  default T transform(List<? extends CVConfig> cvConfigGroup) {
    Preconditions.checkArgument(isNotEmpty(cvConfigGroup), "List of cvConfigs can not empty.");
    Preconditions.checkArgument(cvConfigGroup.stream().map(CVConfig::getIdentifier).distinct().count() == 1,
        "Group ID should be same for List of all configs.");
    List<C> typedCVConfig = (List<C>) cvConfigGroup;
    return transformToHealthSourceConfig(typedCVConfig);
  }
  T transformToHealthSourceConfig(List<C> cvConfigGroup);
}
