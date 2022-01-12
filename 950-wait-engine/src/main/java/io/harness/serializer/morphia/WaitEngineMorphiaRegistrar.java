/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.morphia;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyResponse;
import io.harness.waiter.ProgressUpdate;
import io.harness.waiter.WaitEngineEntity;
import io.harness.waiter.WaitInstance;

import java.util.Set;

@OwnedBy(HarnessTeam.DEL)
public class WaitEngineMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(WaitEngineEntity.class);
    set.add(NotifyEvent.class);
    set.add(NotifyResponse.class);
    set.add(WaitInstance.class);
    set.add(ProgressUpdate.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // No Registrations required
  }
}
