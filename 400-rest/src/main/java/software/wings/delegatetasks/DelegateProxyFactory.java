/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.utils.TaskSetupAbstractionHelper;

import software.wings.beans.SyncTaskContext;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Proxy;

@Singleton
@TargetModule(HarnessModule._910_DELEGATE_SERVICE_DRIVER)
@BreakDependencyOn("software.wings.service.intfc.DelegateService")
public class DelegateProxyFactory {
  @Inject private DelegateService delegateService;
  @Inject private TaskSetupAbstractionHelper taskSetupAbstractionHelper;

  public <T> T get(Class<T> klass, SyncTaskContext syncTaskContext) {
    return (T) Proxy.newProxyInstance(klass.getClassLoader(), new Class[] {klass},
        new DelegateInvocationHandler(syncTaskContext, delegateService, taskSetupAbstractionHelper));
  }
}
