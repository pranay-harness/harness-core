/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.entities;

import io.harness.ChangeHandler;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

@OwnedBy(HarnessTeam.CE)
public interface CDCEntity<T extends PersistentEntity> {
  ChangeHandler getChangeHandler(String handlerClass);
  Class<? extends PersistentEntity> getSubscriptionEntity();
}
