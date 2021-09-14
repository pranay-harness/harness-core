/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.citasks;

import io.harness.delegate.task.citasks.cik8handler.CIK8BuildTaskHandler;
import io.harness.delegate.task.citasks.cik8handler.CIK8CleanupTaskHandler;
import io.harness.delegate.task.citasks.cik8handler.CIK8ExecuteStepTaskHandler;
import io.harness.delegate.task.citasks.cik8handler.K8ExecuteCommandTaskHandler;
import io.harness.threading.Sleeper;
import io.harness.threading.ThreadSleeper;
import io.harness.time.ClockTimer;
import io.harness.time.Timer;

import com.google.inject.AbstractModule;

public class CITaskFactoryModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(CIBuildTaskHandler.class).to(CIK8BuildTaskHandler.class);
    bind(ExecuteCommandTaskHandler.class).to(K8ExecuteCommandTaskHandler.class);
    bind(CICleanupTaskHandler.class).to(CIK8CleanupTaskHandler.class);
    bind(CIExecuteStepTaskHandler.class).to(CIK8ExecuteStepTaskHandler.class);
    bind(Sleeper.class).to(ThreadSleeper.class);
    bind(Timer.class).to(ClockTimer.class);
  }
}
