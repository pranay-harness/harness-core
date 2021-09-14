/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.morphia;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateCallbackRecord;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateScope;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskProgressResponse;
import io.harness.delegate.beans.DelegateToken;
import io.harness.delegate.beans.TaskSelectorMap;
import io.harness.delegate.beans.perpetualtask.PerpetualTaskScheduleConfig;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

@OwnedBy(HarnessTeam.DEL)
public class DelegateServiceBeansMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(DelegateAsyncTaskResponse.class);
    set.add(DelegateCallbackRecord.class);
    set.add(DelegateProfile.class);
    set.add(DelegateScope.class);
    set.add(DelegateSyncTaskResponse.class);
    set.add(DelegateTaskProgressResponse.class);
    set.add(TaskSelectorMap.class);
    set.add(DelegateToken.class);
    set.add(PerpetualTaskScheduleConfig.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // no classes to register
  }
}
