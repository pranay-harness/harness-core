/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
