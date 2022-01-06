/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.waiter.WaiterConfiguration.PersistenceLayer;

import java.lang.annotation.Annotation;
import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public interface WaiterRuleMixin {
  default PersistenceLayer obtainPersistenceLayer(List<Annotation> annotations) {
    return annotations.stream().anyMatch(SpringWaiter.class ::isInstance) ? PersistenceLayer.SPRING
                                                                          : PersistenceLayer.MORPHIA;
  };
}
