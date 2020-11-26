package io.harness.serializer.morphia;

import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateCallbackRecord;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskProgressResponse;
import io.harness.delegate.beans.TaskSelectorMap;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class DelegateServiceBeansMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(DelegateAsyncTaskResponse.class);
    set.add(DelegateCallbackRecord.class);
    set.add(DelegateProfile.class);
    set.add(DelegateSyncTaskResponse.class);
    set.add(DelegateTaskProgressResponse.class);
    set.add(TaskSelectorMap.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // no classes to register
  }
}
